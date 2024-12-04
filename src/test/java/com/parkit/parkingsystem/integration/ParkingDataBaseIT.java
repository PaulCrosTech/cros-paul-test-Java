package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    public static void setUp() throws Exception {
        // ParkingSpotDAO : création data base object + paramétrage sur base de Test
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        // ticketDAO : création data base object + paramétrage sur base de Test
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        // DataBasePrepareService : création de l'objet
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    public void setUpPerTest() throws Exception {

        // Mock : simule la plaque d'immatriculation ABCDEF
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Reset Test Database : delete Tickets, Parking all Available
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {

    }

    @Test
    public void testParkingACar() {
        // GIVEN
        // Mock : simule l'option 1 : CAR
        when(inputReaderUtil.readSelection()).thenReturn(1);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN

        // Vérification de Ticket :
        // Récupère le Ticket en fonction de son numéro de plaque d'immatriculation
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        // Vérifie que le Ticket a été trouvé en BD : la BD étant vierge, il n'y a qu'un seul ticket
        assertNotNull(ticket);

        // Vérification de ParkingSpot :
        // Récupère ParkingSpot en fonction de son ID
        ParkingSpot parkingSpot = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        // Vérifie que la place a été trouvée
        assertNotNull(parkingSpot);
        // Vérifie que la place n'est plus disponible
        assertFalse(parkingSpot.isAvailable());

    }

    @Test
    public void testParkingLotExit() {

        // GIVEN
        // Création d'un ParkingService
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        // Création d'un ParkingSpot
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);

        // Création d'un Ticket
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(ticket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // Vérification de Ticket :
        // Price & Time (not null)
        Ticket ticketInDB = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketInDB);
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticketInDB.getPrice());
        assertNotNull(ticketInDB.getOutTime());

        // Vérification de ParkingSpot :
        // Parking : available
        ParkingSpot parkingSpotInDB = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        assertNotNull(parkingSpotInDB);
        assertTrue(parkingSpotInDB.isAvailable());
    }

    @Test
    public void testParkingLotExitRecurringUser() throws InterruptedException {

        //GIVEN
        when(inputReaderUtil.readSelection()).thenReturn(1); // Choix du type de véhicule : CAR
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // Réalise l'entrée/sortie d'un véhicule pour la première fois
        parkingService.processIncomingVehicle();
//        sleep(1000);
        parkingService.processExitingVehicle();

        // Réalise l'entrée du même véhicule pour la deuxième fois
        // Création d'un Ticket d'une heure
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(ticket);

        //--- WHEN
//        sleep(1000);
        parkingService.processExitingVehicle();

        //--- THEN
        Ticket updatedTicket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(updatedTicket);
        assertNotNull(updatedTicket.getOutTime());
        assertEquals(Math.round(Fare.CAR_RATE_PER_HOUR * 0.95), updatedTicket.getPrice());


    }
}
