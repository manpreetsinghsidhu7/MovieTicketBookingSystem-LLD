package strategy;

import model.Seat;
import model.Show;

public interface PricingStrategy {
    double calculatePrice(Seat seat, Show show);
}
