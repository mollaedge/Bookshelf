import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from '../../service/message/message.service';
import {
  DtoMessageResponse,
  DtoMessageRequest,
  DtoConversationResponse,
} from '../../interfaces/message.interface';
import { PageResponse } from '../../interfaces/page.interface';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-messages',
  templateUrl: './messages.component.html',
  styleUrls: ['./messages.component.scss'],
  standalone: false,
})
export class MessagesComponent implements OnInit, OnDestroy {
  friendId: number | null = null;
  friendName = '';

  conversations: DtoConversationResponse[] = [];
  conversationsLoading = false;

  messages: DtoMessageResponse[] = [];
  messagesLoading = false;
  messageText = '';
  sendingMessage = false;
  currentPage = 0;
  pageSize = 50;
  totalPages = 0;

  private subs: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    this.loadConversations();

    // Get friendId from query params
    const sub = this.route.queryParams.subscribe((params) => {
      const rawFriendId = params['friendId'];
      const parsedFriendId = Number(rawFriendId);
      this.friendId = Number.isFinite(parsedFriendId) && parsedFriendId > 0 ? parsedFriendId : null;

      if (this.friendId) {
        this.updateActiveFriendName();
        this.loadMessages(0);
      } else {
        this.messages = [];
      }
    });
    this.subs.push(sub);

    // Listen to incoming messages
    const msgSub = this.messageService.message$.subscribe((msg) => {
      if (msg.senderId === this.friendId) {
        this.messages.push(msg);
        // Auto-mark as read
        this.messageService.markAsRead(msg.id).subscribe();
        this.scrollToBottom();
      }

      this.loadConversations();
    });
    this.subs.push(msgSub);

    // Listen to read notifications
    const readSub = this.messageService.messageRead$.subscribe((msg) => {
      const idx = this.messages.findIndex((m) => m.id === msg.id);
      if (idx >= 0) {
        this.messages[idx].read = true;
      }

      this.loadConversations();
    });
    this.subs.push(readSub);
  }

  ngOnDestroy(): void {
    this.subs.forEach((s) => s.unsubscribe());
  }

  private loadConversations(): void {
    this.conversationsLoading = true;
    this.messageService.getConversations(0, 30).subscribe({
      next: (response: PageResponse<DtoConversationResponse>) => {
        this.conversations = response.content;
        this.conversationsLoading = false;

        // If no conversation selected, default to first one.
        if (!this.friendId && this.conversations.length > 0) {
          this.openConversation(this.conversations[0]);
          return;
        }

        this.updateActiveFriendName();
      },
      error: () => {
        this.conversationsLoading = false;
      },
    });
  }

  private loadMessages(page: number): void {
    if (!this.friendId) return;

    this.messagesLoading = true;
    this.messageService.getMessages(this.friendId, page, this.pageSize).subscribe({
      next: (response: PageResponse<DtoMessageResponse>) => {
        if (page === 0) {
          this.messages = response.content;
        } else {
          this.messages = [...response.content, ...this.messages];
        }
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        this.messagesLoading = false;
        this.scrollToBottom();
      },
      error: () => {
        this.messagesLoading = false;
      },
    });
  }

  sendMessage(): void {
    if (!this.friendId || !this.messageText.trim()) return;

    this.sendingMessage = true;
    const request: DtoMessageRequest = { content: this.messageText.trim() };

    this.messageService.sendMessage(this.friendId, request).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.messageText = '';
        this.sendingMessage = false;
        this.scrollToBottom();
        this.loadConversations();
      },
      error: () => {
        this.sendingMessage = false;
      },
    });
  }

  openConversation(conversation: DtoConversationResponse): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { friendId: conversation.friendId },
      queryParamsHandling: 'merge',
    });
  }

  isActiveConversation(friendId: number): boolean {
    return this.friendId === friendId;
  }

  deleteMessage(messageId: number): void {
    if (!confirm('Delete this message?')) return;

    this.messageService.deleteMessage(messageId).subscribe({
      next: () => {
        this.messages = this.messages.filter((m) => m.id !== messageId);
      },
    });
  }

  loadOlderMessages(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadMessages(this.currentPage + 1);
    }
  }

  trackByConversationId(_: number, conv: DtoConversationResponse): number {
    return conv.conversationId;
  }

  private updateActiveFriendName(): void {
    if (!this.friendId) {
      this.friendName = '';
      return;
    }

    const activeConversation = this.conversations.find((c) => c.friendId === this.friendId);
    this.friendName = activeConversation?.friendName ?? 'Chat';
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const container = document.querySelector('.messages-container');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }, 100);
  }

  timeAgo(isoString: string): string {
    const diff = Date.now() - new Date(isoString).getTime();
    const m = Math.floor(diff / 60000);
    if (m < 1) return 'Just now';
    if (m < 60) return `${m}m ago`;
    const h = Math.floor(m / 60);
    if (h < 24) return `${h}h ago`;
    return `${Math.floor(h / 24)}d ago`;
  }
}
