export interface DtoReplySnippet {
  id: number;
  senderId: number;
  senderName: string;
  contentSnippet: string;
}

export interface DtoMessageRequest {
  content: string;
  replyToId?: number | null;
}

export interface DtoMessageResponse {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  content: string;
  read: boolean;
  createdAt: Date;
  replyTo?: DtoReplySnippet | null;
}

export interface DtoConversationResponse {
  conversationId: number;
  friendId: number;
  friendName: string;
  lastMessagePreview: string | null;
  lastMessageAt: Date;
  unreadCount: number;
}
