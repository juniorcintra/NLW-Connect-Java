package br.com.nlw.events.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.nlw.events.dto.SubscriptionRankingByUser;
import br.com.nlw.events.dto.SubscriptionRankingItem;
import br.com.nlw.events.dto.SubscriptionResponse;
import br.com.nlw.events.exception.EventNotFoundException;
import br.com.nlw.events.exception.SubscriptionConflictException;
import br.com.nlw.events.exception.UserIndicadorNotFoundException;
import br.com.nlw.events.model.Event;
import br.com.nlw.events.model.Subscription;
import br.com.nlw.events.model.User;
import br.com.nlw.events.repository.EventRepository;
import br.com.nlw.events.repository.SubscriptionRepository;
import br.com.nlw.events.repository.UserRepository;

@Service
public class SubscriptionService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public SubscriptionResponse createNewSubscription(String eventName, User user, Integer userId) {
        Event event = eventRepository.findByPrettyName(eventName);
        if (event == null) {
            throw new EventNotFoundException("Evento " + eventName + " não existe");
        }
        User userFound = userRepository.findByEmail(user.getEmail());
        if (userFound == null) {
            userFound = userRepository.save(user);
        }

        User indicador = null;
        if (userId != null) {
            indicador = userRepository.findById(userId).orElse(null);
            if (indicador == null) {
                throw new UserIndicadorNotFoundException(
                        "Usuário " + userId + " indicador não existe");
            }
        }

        Subscription subs = new Subscription();
        subs.setEvent(event);
        subs.setSubscriber(userFound);
        subs.setIndication(indicador);

        Subscription tmpSub = subscriptionRepository.findByEventAndSubscriber(event, userFound);
        if (tmpSub != null) {
            throw new SubscriptionConflictException("Já existe incrição para o usuário "
                    + userFound.getName() + " no evento " + event.getTitle());
        }

        Subscription res = subscriptionRepository.save(subs);
        return new SubscriptionResponse(res.getSubscriptionNumber(),
                "http://codecraft.com/subscription/" + res.getEvent().getPrettyName() + "/"
                        + res.getSubscriber().getId());
    }

    public List<SubscriptionRankingItem> getCompleteRanking(String prettyName) {
        Event event = eventRepository.findByPrettyName(prettyName);
        if (event == null) {
            throw new EventNotFoundException("Ranking do evento " + prettyName + " não existe");
        }
        return subscriptionRepository.generateRanking(event.getEventId());
    }

    public SubscriptionRankingByUser getRankingByUser(String prettyName, Integer userId) {
        List<SubscriptionRankingItem> ranking = getCompleteRanking(prettyName);
        SubscriptionRankingItem item =
                ranking.stream().filter(i -> i.userId().equals(userId)).findFirst().orElse(null);
        if (item == null) {
            throw new UserIndicadorNotFoundException(
                    "Não há inscrições com indicação do usuário " + userId);
        }
        Integer posicao = IntStream.range(0, ranking.size())
                .filter(pos -> ranking.get(pos).userId().equals(userId)).findFirst().getAsInt();
        return new SubscriptionRankingByUser(item, posicao + 1);
    }
}
