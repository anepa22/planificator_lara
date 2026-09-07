package ar.com.anepanet.planificator.realtime;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class RealtimeBroadcastService {

    private final PlanificatorWebSocketHandler webSocketHandler;

    public RealtimeBroadcastService(PlanificatorWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlanificatorChanged(PlanificatorChangedEvent event) {
        webSocketHandler.broadcast(event.scope());
    }
}
