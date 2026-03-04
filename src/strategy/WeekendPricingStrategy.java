package strategy;

import model.Seat;
import model.Show;

public class WeekendPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(Seat seat, Show show) {
        switch (seat.getType()) {
            case REGULAR:
                return 150.0;
            case PREMIUM:
                return 200.0;
            case VIP:
                return 350.0;
            default:
                return 150.0;
        }
    }
}
