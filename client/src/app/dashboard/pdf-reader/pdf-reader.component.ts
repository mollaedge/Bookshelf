import { Component, OnInit, OnDestroy, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-pdf-reader',
  templateUrl: './pdf-reader.component.html',
  styleUrls: ['./pdf-reader.component.scss'],
  standalone: false
})
export class PdfReaderComponent implements OnInit, OnDestroy {
  pdfSrc: string = '';
  safeUrl: SafeResourceUrl | null = null;
  currentPage: number = 1;
  zoom: number = 100;
  isBrowser: boolean;
  loadError: boolean = false;

  private routeSub!: Subscription;

  constructor(
    private route: ActivatedRoute,
    private sanitizer: DomSanitizer,
    @Inject(PLATFORM_ID) platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    this.routeSub = this.route.queryParams.subscribe(params => {
      if (params['src']) {
        this.pdfSrc = decodeURIComponent(params['src']);
        this.loadError = false;
      }
      if (params['page']) {
        const page = parseInt(params['page'], 10);
        if (!isNaN(page) && page > 0) {
          this.currentPage = page;
        }
      }
      this.buildSafeUrl();
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private buildSafeUrl(): void {
    if (!this.pdfSrc) { this.safeUrl = null; return; }
    const url = `${this.pdfSrc}#page=${this.currentPage}&zoom=${this.zoom}`;
    this.safeUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }

  goToPage(page: number): void {
    if (page >= 1) {
      this.currentPage = page;
      this.buildSafeUrl();
    }
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.buildSafeUrl();
    }
  }

  nextPage(): void {
    this.currentPage++;
    this.buildSafeUrl();
  }

  zoomIn(): void {
    if (this.zoom < 300) {
      this.zoom += 20;
      this.buildSafeUrl();
    }
  }

  zoomOut(): void {
    if (this.zoom > 40) {
      this.zoom -= 20;
      this.buildSafeUrl();
    }
  }

  resetZoom(): void {
    this.zoom = 100;
    this.buildSafeUrl();
  }

  onPageInput(event: Event): void {
    const value = parseInt((event.target as HTMLInputElement).value, 10);
    if (!isNaN(value)) {
      this.goToPage(value);
    }
  }

  onLoadError(): void {
    this.loadError = true;
  }
}
