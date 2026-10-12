package et.medco.etrade.contribution;

public enum PaymentStatus {
    /** Created and awaiting settlement. */
    PENDING,
    /** Settled. In this demo that means someone called the confirm endpoint. */
    PAID
}
