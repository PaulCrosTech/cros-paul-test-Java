package com.parkit.parkingsystem.integration;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
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

    //TODO: check that the fare generated and out time are populated correctly in the database
    @Test
    public void testParkingLotExit() {
        // GIVEN
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        // GIVEN
        // Création d'un ParkingSpot
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);

        // Création d'un Ticket
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(1.5);
        ticketDAO.saveTicket(ticket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // Vérifition de Ticket :
        // Price & Time (not null)
        Ticket ticketInDB = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketInDB);
        assertEquals(1.5, ticketInDB.getPrice());
        assertNotNull(ticketInDB.getOutTime());

        // Vérification de ParkingSpot :
        // Parking : available
        ParkingSpot parkingSpotInDB = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        assertNotNull(parkingSpotInDB);
        assertTrue(parkingSpotInDB.isAvailable());

    }

}
