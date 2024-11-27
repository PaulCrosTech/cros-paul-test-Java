package com.parkit.parkingsystem;

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

    @Test
    public void processExitingVehicleTest() {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test mock objects");
        }
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        // Création d'un ParkingSpot
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        // Création d'un Ticket
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        // Mock la méthode getTicket
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        // Mock la méthode updateTicket (maj prix et date de sortie)
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
        // Mock la méthode updateParking (maj disponibilité, la place devient libre)
        when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // vérifie que la méthode getTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).getTicket(anyString());
        // vérifie que la méthode getNbTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        // vérifie que la méthode updateTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        // vérifie que la méthode updateParking a été appelée une fois
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processExitingVehicleTestUnableUpdate() {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test mock objects");
        }
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        // Création d'un ParkingSpot
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        // Création d'un Ticket
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        // Mock la méthode getTicket
        when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
        // Mock la méthode updateTicket (maj prix et date de sortie)
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // vérifie que la méthode getTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).getTicket(anyString());
        // vérifie que la méthode getNbTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
        // vérifie que la méthode updateTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
        // vérifie que la méthode updateParking n'a pas été appelée
        verify(parkingSpotDAO, Mockito.times(0)).updateParking(any(ParkingSpot.class));
    }

    @Test
    public void processIncomingVehicleTest() {
        // GIVEN
        // Mock input reader, renvoie "ABCDEF" pour le numéro d'immatriculation
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        } catch (Exception e) {
            throw new RuntimeException("Failed to set up test mock objects");
        }
        // Mock la méthode getNbTicket
        when(ticketDAO.getNbTicket(anyString())).thenReturn(0);
        // Mock input reader, renvoie "1" pour le type de véhicule CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        // Mock input reader, renvoie "1" pour le numéro du parking disponible
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        // vérifie que la méthode getNextAvailableSlot a été appelée une fois
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        // vérifie que la méthode updateParking a été appelée une fois
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
        // vérifie que la méthode saveTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
        // vérifie que la méthode getNbTicket a été appelée une fois
        verify(ticketDAO, Mockito.times(1)).getNbTicket(anyString());
    }

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
        // vérifie que la méthode getNextAvailableSlot a été appelée une fois
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        assertEquals(1, parkingSpot.getId());
        assertTrue(parkingSpot.isAvailable());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
    }

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
        // vérifie que la méthode getNextAvailableSlot a été appelée une fois
        verify(parkingSpotDAO, Mockito.times(1)).getNextAvailableSlot(any(ParkingType.class));
        // vérifie que le parkingSpot est null
        assertNull(parkingSpot);

    }

    @Test
    public void testGetNextParkingNumberIfAvailableParkingNumberWrongArgument() {
        // GIVEN
        // Mock input reader, renvoie "3" pour un type de véhicule incorrect
        when(inputReaderUtil.readSelection()).thenReturn(3);

        // WHEN
        ParkingSpot parkingSpot = parkingService.getNextParkingNumberIfAvailable();

        // THEN
        // vérifie que la méthode getNextAvailableSlot n'a pas été appelée
        verify(parkingSpotDAO, Mockito.times(0)).getNextAvailableSlot(any(ParkingType.class));
        // vérifie que le parkingSpot est null
        assertNull(parkingSpot);

    }
}
