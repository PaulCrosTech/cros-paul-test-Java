package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    public void setUpPerTest() {
        // Crée le service de parking avec les mocks
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
    }

    /**
     * Etape #5 : Test de la méthode processExitingVehicle
     */
    @Test
    public void processExitingVehicleTest() throws Exception {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Mock la méthode getTicket, renvoi un Ticket de 60minutes
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        // Mock la méthode updateTicket (maj prix et date de sortie)
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        // Mock la méthode updateParking (maj disponibilité, la place devient libre)
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, Mockito.times(1)).getTicket(anyString());
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    /**
     * Etape #5 : Test de la méthode processExitingVehicle, updateTicket impossible
     */
    @Test
    public void processExitingVehicleTestUnableUpdate() throws Exception {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Mock la méthode getTicket, renvoie un Ticket de 60minutes
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        // Mock la méthode updateTicket (maj impossible)
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        verify(ticketDAO, Mockito.times(1)).getTicket(anyString());
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
    }

    /**
     * Etape #5 : Test de la méthode processExitingVehicle
     */
    @Test
    public void testProcessIncomingVehicle() throws Exception {
        // GIVEN
        // Mock input reader, renvoie "1" pour le type de véhicule CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // Mock input reader, renvoie "1" pour le numéro du parking disponible
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Mock la méthode updateParking, renvoi True pour indiquer la mise à jour de la BD
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
        // Mock la méthode saveTicket
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }

    /**
     * Etape #5 : Test de la méthode processIncomingVehicle, client régulier
     * Rajoute pour une meilleure couverture de code
     */
    @Test
    public void processIncomingVehicleForRegularClientTest() throws Exception {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
        // Mock la méthode saveTicket
        when(ticketDAO.saveTicket(any(Ticket.class))).thenReturn(true);
        // Mock input reader, renvoie "1" pour le type de véhicule CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // Mock input reader, renvoie "1" pour le numéro du parking disponible
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }

    /**
     * Etape #5 : Test de la méthode getNextParkingNumberIfAvailable
     */
    @Test
    public void testGetNextParkingNumberIfAvailable() {
        // GIVEN
        // Mock input reader, renvoie "1" pour le type de véhicule CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // Mock la méthode getNextAvailableSlot
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertEquals(1, parkingSpot.getId());
        assertTrue(parkingSpot.isAvailable());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
    }

    /**
     * Etape #5 : Test de la méthode getNextParkingNumberIfAvailable, parking complet
     */
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberNotFound() {

        // GIVEN
        // Mock input reader, renvoie "1" pour le type de véhicule CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // Mock la méthode getNextAvailableSlot
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertNull(parkingSpot);
    }

    /**
     * Etape #5 : Test de la méthode getNextParkingNumberIfAvailable, type de véhicule incorrect
     */
    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        // GIVEN
        // Mock input reader, renvoie "3" pour un type de véhicule incorrect
        when(inputReaderUtil.readSelection()).thenReturn(3);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        verify(parkingSpotDAO, Mockito.times(0)).getNextAvailableSlot(any(ParkingType.class));
        assertNull(parkingSpot);

    }

}
