import { formatInstant } from '../lib/dateUtils';

interface DateDisplayProps {
  instantString: string;
  format?: 'full' | 'date' | 'time' | 'datetime';
  locale?: string;
}

/**
 * Component để hiển thị thời gian từ backend Instant string
 * Tự động convert sang múi giờ địa phương
 */
export function DateDisplay({ instantString, format = 'full', locale = 'vi-VN' }: DateDisplayProps) {
  if (!instantString) return <span>-</span>;

  const formatOptions: Record<string, Intl.DateTimeFormatOptions> = {
    full: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    },
    datetime: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    },
    date: {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    },
    time: {
      hour: '2-digit',
      minute: '2-digit',
    },
  };

  const formatted = formatInstant(instantString, locale, formatOptions[format]);

  return <span>{formatted}</span>;
}

/**
 * Hook để format Instant string thành local date string
 */
export function useFormatInstant() {
  return {
    formatFull: (instantString: string) => formatInstant(instantString, 'vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    formatDate: (instantString: string) => formatInstant(instantString, 'vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }),
    formatTime: (instantString: string) => formatInstant(instantString, 'vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
    }),
    formatDateTime: (instantString: string) => formatInstant(instantString, 'vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }),
  };
}
