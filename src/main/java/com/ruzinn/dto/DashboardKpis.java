package com.ruzinn.dto;

/**
 * Dashboard tiles for the signed-in company.
 *
 * @param bookingsLast30Days bookings created in the last 30 days (today included)
 * @param pendingParks park items still PENDING or REQUESTED
 * @param ticketsIssued ticket-type items (not hotel / visa) with a ticket uploaded
 * @param pendingTickets ticket-type items still waiting for a ticket
 */
public record DashboardKpis(int bookingsLast30Days, int pendingParks, int ticketsIssued, int pendingTickets) {}
