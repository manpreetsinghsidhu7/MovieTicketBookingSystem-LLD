package strategy;

import model.Seat;
import model.Show;

public class RegularPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(Seat seat, Show show) {
        switch (seat.getType()) {
            case REGULAR:
                return 100.0;
            case PREMIUM:
                return 150.0;
            case VIP:
                return 250.0;
            default:
                return 100.0;
        }
    }
}
