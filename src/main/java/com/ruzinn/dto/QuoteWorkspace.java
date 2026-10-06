package com.ruzinn.dto;

import java.util.List;

/**
 * Everything the quote detail screen needs in one call.
 *
 * @param quote the quote as the viewer may see it
 * @param showActualRates parent-company users also see the actual (net) rates
 * @param canManageQuote whether the viewer may change the status and follow-up
 * @param statusOptions statuses the viewer can pick
 */
public record QuoteWorkspace(
    QuoteMainView quote, boolean showActualRates, boolean canManageQuote, List<String> statusOptions) {}
