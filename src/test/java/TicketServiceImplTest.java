import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)

public class TicketServiceImplTest {

    @Mock
    private TicketPaymentServiceImpl ticketPaymentService;

    @Mock
    private SeatReservationServiceImpl seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    public void testPurchaseTicketsSuccess() throws InvalidPurchaseException {
        TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[]{
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)
        };

        ticketService.purchaseTickets(1L, ticketTypeRequests);

        verify(ticketPaymentService, times(1)).makePayment(1L, 80);
        verify(seatReservationService, times(1)).reserveSeat(1L, 5);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void testPurchaseTicketsInvalidInputs() throws InvalidPurchaseException {
        TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[0];

        ticketService.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void testPurchaseTicketsNoAdultTicket() throws InvalidPurchaseException {
        TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[]{
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)
        };

        ticketService.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void testPurchaseTicketsTooManyTickets() throws InvalidPurchaseException {
        TicketTypeRequest[] ticketTypeRequests = new TicketTypeRequest[]{
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 7)
        };

        ticketService.purchaseTickets(1L, ticketTypeRequests);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void testInvalidAccountId() throws InvalidPurchaseException {
        ticketService.purchaseTickets(null, new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test(expected = InvalidPurchaseException.class)
    public void purchaseTickets_moreInfantsThanAdults_throwsException() throws InvalidPurchaseException {
        TicketTypeRequest[] ticketTypeRequests = {
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 5),
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4)
        };

        try {
            ticketService.purchaseTickets(1L, ticketTypeRequests);
            fail("Expected InvalidPurchaseException");
        } catch (InvalidPurchaseException e) {
            verify(ticketPaymentService, never()).makePayment(anyLong(), anyInt());
            verify(seatReservationService, never()).reserveSeat(anyLong(), anyInt());
            throw e;
        }
    }

}