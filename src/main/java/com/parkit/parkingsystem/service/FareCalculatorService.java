package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect:" + ticket.getOutTime().toString());
        }

        // Pour corriger le bug de l'étape #1, on utilise l'heure d'entrée et sortie en millisecondes
        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();

        // Calcul de la durée précise (en nombre décimal)
        double duration = (outHour - inHour) / 1000 / 60 / 60;


        // Arrondi le résultat à deux chiffres après la virgule
        double durationRounded = new BigDecimal(duration).setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Etape #3 : implémentation des 30 premières minutes sont gratuites
        if (durationRounded <= 0.5) {
            durationRounded = 0;
        }

        double finalPrice;
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                finalPrice = durationRounded * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                finalPrice = durationRounded * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }

        // Applique la remise de 5% si le client est déjà venu
        if (discount) {
            finalPrice *= 0.95;
        }

        ticket.setPrice(finalPrice);
    }
}