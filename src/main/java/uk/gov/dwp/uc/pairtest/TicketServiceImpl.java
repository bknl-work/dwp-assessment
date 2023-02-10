package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private TicketPaymentServiceImpl ticketPaymentService;
    private SeatReservationServiceImpl seatReservationService;

    public TicketServiceImpl(TicketPaymentServiceImpl ticketPaymentService, SeatReservationServiceImpl seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        try {
            validateInputs(accountId, ticketTypeRequests);

            int cost = calculateCost(ticketTypeRequests);
            int seats = calculateSeats(ticketTypeRequests);

            processPayment(accountId, cost);
            reserveSeats(accountId, seats);
        }
        catch (InvalidPurchaseException e) {
            throw e;
        }
    }

    private void validateInputs(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0)
            throw new InvalidPurchaseException("No valid ticket type requests provided");
        if (accountId == null || accountId <= 0)
            throw new InvalidPurchaseException("Invalid account id provided");

        boolean isAdultPresent = false;
        int ticketAmount = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            ticketAmount += ticketTypeRequest.getNoOfTickets();
            if (ticketAmount > 20)
                throw new InvalidPurchaseException("Too many tickets being purchased at once, please lower the number of tickets");
            if (ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.ADULT)
                isAdultPresent = true;
        }

        if (!isAdultPresent)
            throw new InvalidPurchaseException("No adult tickets are present, Please add a adult ticket");
    }

    private int calculateCost(TicketTypeRequest... ticketTypeRequests) {
        int cost = 0;

        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            switch (ticketTypeRequest.getTicketType()) {
                case CHILD:
                    cost += ticketTypeRequest.getNoOfTickets() * 10;
                    break;
                case ADULT:
                    cost += ticketTypeRequest.getNoOfTickets() * 20;
                    break;
            }
        }

        return cost;
    }

    private int calculateSeats(TicketTypeRequest... ticketTypeRequests) {
        int[] seats = new int[3];
        Arrays.stream(ticketTypeRequests).forEach(ttr -> seats[ttr.getTicketType().ordinal()] += ttr.getNoOfTickets());

        if (seats[TicketTypeRequest.Type.INFANT.ordinal()] > seats[TicketTypeRequest.Type.ADULT.ordinal()])
            throw new InvalidPurchaseException("Too many infant tickets, there should be at least 1 adult for every infant");

        return seats[TicketTypeRequest.Type.ADULT.ordinal()] + seats[TicketTypeRequest.Type.CHILD.ordinal()];
    }

    private void processPayment(Long accountId, int cost) throws InvalidPurchaseException {
        try {
            ticketPaymentService.makePayment(accountId, cost);
        } catch (Exception e) {
            throw new InvalidPurchaseException(e.getMessage());
        }
    }

    private void reserveSeats(Long accountId, int seats) throws InvalidPurchaseException {
        try {
            seatReservationService.reserveSeat(accountId, seats);
        } catch (Exception e) {
            throw new InvalidPurchaseException(e.getMessage());
        }
    }


}
