/**
 * Utility functions for handling date/time conversion between frontend and backend
 * Backend uses Java Instant (UTC timestamps in ISO-8601 format)
 * Frontend displays dates in local timezone
 */

/**
 * Convert a local Date to ISO-8601 string for backend Instant
 * @param date - Local Date object or date string
 * @returns ISO-8601 string in UTC (e.g., "2025-12-07T17:00:00.000Z")
 */
export function toInstantString(date: Date | string): string {
  const dateObj = typeof date === 'string' ? new Date(date) : date;
  return dateObj.toISOString();
}

/**
 * Convert backend Instant string to local Date object
 * @param instantString - ISO-8601 string from backend (e.g., "2025-12-07T17:00:00.000Z")
 * @returns Date object in local timezone
 */
export function fromInstantString(instantString: string): Date {
  return new Date(instantString);
}

/**
 * Format an Instant string to local date string
 * @param instantString - ISO-8601 string from backend
 * @param locale - Locale for formatting (default: 'vi-VN')
 * @returns Formatted date string in local timezone
 */
export function formatInstant(
  instantString: string,
  locale: string = 'vi-VN',
  options?: Intl.DateTimeFormatOptions
): string {
  const date = fromInstantString(instantString);
  const defaultOptions: Intl.DateTimeFormatOptions = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    ...options,
  };
  return date.toLocaleString(locale, defaultOptions);
}

/**
 * Convert a date string from datetime-local input to Instant string
 * @param dateTimeLocal - String from <input type="datetime-local"> (e.g., "2025-12-07T17:00")
 * @returns ISO-8601 string in UTC
 */
export function dateTimeLocalToInstant(dateTimeLocal: string): string {
  // datetime-local input gives local time without timezone
  // We need to treat it as local time and convert to UTC
  const date = new Date(dateTimeLocal);
  return date.toISOString();
}

/**
 * Convert an Instant string to datetime-local input format
 * @param instantString - ISO-8601 string from backend
 * @returns String for <input type="datetime-local"> in local timezone (e.g., "2025-12-07T17:00")
 */
export function instantToDateTimeLocal(instantString: string): string {
  const date = fromInstantString(instantString);
  // Format: YYYY-MM-DDTHH:mm
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${year}-${month}-${day}T${hours}:${minutes}`;
}

/**
 * Get start of day in UTC for a given local date
 * @param date - Local Date object
 * @returns ISO-8601 string representing start of day in UTC
 */
export function startOfDayUTC(date: Date): string {
  const startOfDay = new Date(date);
  startOfDay.setHours(0, 0, 0, 0);
  return startOfDay.toISOString();
}

/**
 * Get end of day in UTC for a given local date
 * @param date - Local Date object
 * @returns ISO-8601 string representing end of day in UTC
 */
export function endOfDayUTC(date: Date): string {
  const endOfDay = new Date(date);
  endOfDay.setHours(23, 59, 59, 999);
  return endOfDay.toISOString();
}
