import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from '../../service/message/message.service';
import {
  DtoMessageResponse,
  DtoMessageRequest,
  DtoConversationResponse,
  DtoReplySnippet,
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
  activeConversationId: number | null = null;
  friendName = '';

  conversations: DtoConversationResponse[] = [];
  conversationsLoading = false;

  messages: DtoMessageResponse[] = [];
  messagesLoading = false;
  messageText = '';
  sendingMessage = false;
  replyingTo: DtoReplySnippet | null = null;
  currentPage = 0;
  pageSize = 50;
  totalPages = 0;
  selectedFile: File | null = null;
  selectedFilePreview: string | null = null;
  zoomedImage: string | null = null;
  isSidebarCollapsed = false;

  private subs: Subscription[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private messageService: MessageService
  ) {}

  ngOnInit(): void {
    // Ensure SSE is connected even if NavComponent hasn't done it yet
    // (idempotent – does nothing if already connected)
    this.messageService.connectSSE();

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
      // Update the conversation sidebar locally (no HTTP round-trip)
      this._applyIncomingToSidebar(msg);

      // Append to open chat if it belongs to the active conversation
      if (this.activeConversationId !== null && msg.conversationId === this.activeConversationId) {
        // Avoid duplicates (e.g. own message already pushed in sendMessage)
        if (!this.messages.some((m) => m.id === msg.id)) {
          this.messages.push(msg);
          this.scrollToBottom();
        }
        // Auto-mark as read since the user is looking at this conversation
        if (msg.senderId === this.friendId) {
          this.messageService.markAsRead(msg.id).subscribe();
        }
      }
    });
    this.subs.push(msgSub);

    // Listen to read notifications
    const readSub = this.messageService.messageRead$.subscribe((msg) => {
      const idx = this.messages.findIndex((m) => m.id === msg.id);
      if (idx >= 0) {
        this.messages[idx] = { ...this.messages[idx], read: true };
      }
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

        // Cache the active conversation ID for SSE matching
        const active = this.conversations.find((c) => c.friendId === this.friendId);
        this.activeConversationId = active?.conversationId ?? null;

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

        // After loading messages (which marks them as read), zero out unreadCount locally
        const idx = this.conversations.findIndex(c => c.friendId === this.friendId);
        if (idx >= 0) {
          this.conversations[idx] = { ...this.conversations[idx], unreadCount: 0 };
        }
        // Notify other components (like Navbar) to refresh their badges
        this.messageService.notifyUnreadCountChanged();
      },
      error: () => {
        this.messagesLoading = false;
      },
    });
  }

  setReply(msg: DtoMessageResponse): void {
    this.replyingTo = {
      id: msg.id,
      senderId: msg.senderId,
      senderName: msg.senderName,
      contentSnippet: msg.content.length > 80 ? msg.content.slice(0, 80) + '…' : msg.content,
    };
  }

  cancelReply(): void {
    this.replyingTo = null;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.selectedFile = input.files[0];

      // Create preview if it's an image
      if (this.selectedFile.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (e) => {
          this.selectedFilePreview = e.target?.result as string;
        };
        reader.readAsDataURL(this.selectedFile);
      } else {
        this.selectedFilePreview = null;
      }
    }
  }

  removeSelectedFile(): void {
    this.selectedFile = null;
    this.selectedFilePreview = null;
  }

  sendMessage(): void {
    const hasText = this.messageText.trim().length > 0;
    const hasFile = !!this.selectedFile;

    if (!this.friendId || (!hasText && !hasFile)) return;

    this.sendingMessage = true;
    const request: DtoMessageRequest & { media?: File | null } = {
      content: this.messageText.trim(),
      replyToId: this.replyingTo?.id ?? null,
      media: this.selectedFile,
    };

    this.messageService.sendMessage(this.friendId, request).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        // Keep activeConversationId in sync for first message in a new convo
        if (!this.activeConversationId) {
          this.activeConversationId = msg.conversationId;
        }
        this.messageText = '';
        this.replyingTo = null;
        this.removeSelectedFile();
        this.sendingMessage = false;
        this.scrollToBottom();
        // Refresh sidebar so preview + timestamp update
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

  /** Update conversation sidebar in-place from an incoming SSE message (no HTTP call). */
  private _applyIncomingToSidebar(msg: DtoMessageResponse): void {
    const idx = this.conversations.findIndex((c) => c.conversationId === msg.conversationId);
    if (idx < 0) {
      // New conversation not yet in list — reload to get it
      this.loadConversations();
      return;
    }

    const conv = this.conversations[idx];
    const isActive = conv.conversationId === this.activeConversationId;

    this.conversations[idx] = {
      ...conv,
      lastMessagePreview: msg.content,
      lastMessageAt: msg.createdAt,
      // If user is viewing this conversation the message is immediately read
      unreadCount: isActive ? 0 : conv.unreadCount + 1,
    };

    if (isActive) {
      this.messageService.notifyUnreadCountChanged();
    }

    // Bubble updated conversation to the top
    const updated = this.conversations.splice(idx, 1)[0];
    this.conversations.unshift(updated);
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
    const rem = m % 60;
    if (h < 24) return rem > 0 ? `${h}h ${rem}m ago` : `${h}h ago`;
    const d = Math.floor(h / 24);
    const remH = h % 24;
    return remH > 0 ? `${d}d ${remH}h ago` : `${d}d ago`;
  }

  zoomImage(data: string): void {
    this.zoomedImage = data;
  }

  closeZoom(): void {
    this.zoomedImage = null;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}
