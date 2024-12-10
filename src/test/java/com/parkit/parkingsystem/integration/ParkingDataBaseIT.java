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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static final DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static ParkingService parkingService;

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

        // Mock : simule la plaque d'immatriculation 'ABCDEF'
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        // Création de l'objet ParkingService
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        // Reset Test Database : delete Tickets, Parking all Available
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    public static void tearDown() {
        // Reset Test Database : delete Tickets, Parking all Available
        //        dataBasePrepareService.clearDataBaseEntries();
    }

    /**
     * Etape #6 : Test de stationnement d'un véhicule dans le parking
     */
    @Test
    public void testParkingACar() {
        // GIVEN
        // Mock : simule l'option 1 'CAR'
        when(inputReaderUtil.readSelection()).thenReturn(1);

        // WHEN
        parkingService.processIncomingVehicle();

        // THEN
        // Vérification de Ticket
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket);
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(0, ticket.getPrice());

        // Vérification de ParkingSpot
        ParkingSpot parkingSpot = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        assertNotNull(parkingSpot);
        assertFalse(parkingSpot.isAvailable());
    }


    /**
     * Etape #6 : Test de sortie d'un véhicule du parking
     */
    @Test
    public void testParkingLotExit() {

        // GIVEN
        // Création d'un Ticket de 60 minutes
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(parkingSpot);
        Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(ticket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // Vérification de Ticket
        Ticket ticketInDB = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketInDB);
        assertNotNull(ticketInDB.getInTime());
        assertNotNull(ticketInDB.getOutTime());
        assertEquals((Fare.CAR_RATE_PER_HOUR), ticketInDB.getPrice());

        // Vérification de ParkingSpot
        ParkingSpot parkingSpotInDB = parkingSpotDAO.getParkingSpotById(ticket.getParkingSpot().getId());
        assertNotNull(parkingSpotInDB);
        assertTrue(parkingSpotInDB.isAvailable());
    }

    /**
     * Etape #6 : Test de sortie d'un véhicule du parking pour un utilisateur récurrent
     */
    @Test
    public void testParkingLotExitRecurringUser() {

        // GIVEN
        // Création d'un premier Ticket : pour simuler le fait que le client est déjà venu
        // On laisse la place de parking disponible, pour simuler le fait qu'il a quitté le parking
        ParkingSpot firstParkingSpot = new ParkingSpot(1, ParkingType.CAR, true);
        parkingSpotDAO.updateParking(firstParkingSpot);
        Date inTime = new Date(System.currentTimeMillis() - (24 * 60 * 60 * 1000)); // Date à hier
        Date outTime = new Date(inTime.getTime() + (30 * 60 * 1000));  // 30 minutes après inTime
        Ticket firstTicket = new Ticket();
        firstTicket.setPrice(0);
        firstTicket.setInTime(inTime);
        firstTicket.setOutTime(outTime);
        firstTicket.setParkingSpot(firstParkingSpot);
        firstTicket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(firstTicket);

        // Création d'un deuxième Ticket : pour simuler le fait que le client est revenu ce jour
        ParkingSpot secondParkingSpot = new ParkingSpot(1, ParkingType.CAR, false);
        parkingSpotDAO.updateParking(secondParkingSpot);
        Ticket secondTicket = new Ticket();
        secondTicket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 60 minutes
        secondTicket.setParkingSpot(secondParkingSpot);
        secondTicket.setVehicleRegNumber("ABCDEF");
        ticketDAO.saveTicket(secondTicket);

        // WHEN
        parkingService.processExitingVehicle();

        // THEN
        // Vérifie qu'il y a 2 Ticket pour le véhicule 'ABCDEF'
        assertEquals(2, ticketDAO.getNbTicket("ABCDEF"));
        // Vérification du second Ticket
        Ticket ticketInDB = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticketInDB);
        assertNotNull(ticketInDB.getInTime());
        assertNotNull(ticketInDB.getOutTime());
        assertEquals((Fare.CAR_RATE_PER_HOUR * 0.95), ticketInDB.getPrice());

        // Vérification de ParkingSpot
        ParkingSpot parkingSpotInDB = parkingSpotDAO.getParkingSpotById(ticketInDB.getParkingSpot().getId());
        assertNotNull(parkingSpotInDB);
        assertTrue(parkingSpotInDB.isAvailable());

    }
}