package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket) {
        calculateFare(ticket, false);
    }

    /**
     * Etape #4 : Surchage du constructeur, ajout d'un paramètre discount pour appliquer une remise de 5%
     */
    public void calculateFare(Ticket ticket, boolean discount) {
        if ((ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime()))) {
            throw new IllegalArgumentException("Out time provided is incorrect : " + ticket.getOutTime().toString() + " < In time " + ticket.getInTime().toString());
        }

        // Etape #1 : Correction du bug de la durée de stationnement, on utilise un calcul en millisecondes
        double inHour = ticket.getInTime().getTime();
        double outHour = ticket.getOutTime().getTime();
        double duration = (outHour - inHour) / 1000 / 60 / 60;
        duration = BigDecimal.valueOf(duration).setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Etape #3 : implémentation des '30 minutes gratuites'
        if (duration <= 0.5) {
            duration = 0;
        }

        double finalPrice;
        switch (ticket.getParkingSpot().getParkingType()) {
            case CAR: {
                finalPrice = duration * Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                finalPrice = duration * Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default:
                throw new IllegalArgumentException("Unkown Parking Type");
        }

        // Etape #4 : Applique la remise de 5%
        if (discount) {
            finalPrice *= 0.95;
        }

        ticket.setPrice(finalPrice);

    }
}