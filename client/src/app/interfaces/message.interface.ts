export interface DtoMessageRequest {
  content: string;
}

export interface DtoMessageResponse {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  content: string;
  read: boolean;
  createdAt: Date;
}

export interface DtoConversationResponse {
  conversationId: number;
  friendId: number;
  friendName: string;
  lastMessagePreview: string | null;
  lastMessageAt: Date;
  unreadCount: number;
}
