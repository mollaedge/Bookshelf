export type NotificationType =
  | 'BOOK_BORROWED'
  | 'BOOK_RETURNED'
  | 'BOOK_REQUEST'
  | 'REQUEST_APPROVED'
  | 'REQUEST_REJECTED'
  | 'BOOK_BORROW_REJECTED'
  | 'NEW_FOLLOWER'
  | 'POST_LIKE'
  | 'POST_COMMENT'
  | 'SYSTEM'
  | string;

export interface DtoNotificationResponse {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  read: boolean;
  referenceId: number | null;
  referenceType: string | null;
  actorId: number | null;
  actorName: string | null;
  createdAt: string; // ISO LocalDateTime string
}
