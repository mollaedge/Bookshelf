import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PdfReaderComponent } from './pdf-reader.component';

const routes: Routes = [
  { path: '', component: PdfReaderComponent }
];

@NgModule({
  declarations: [PdfReaderComponent],
  imports: [
    CommonModule,
    FormsModule,
    RouterModule.forChild(routes)
  ]
})
export class PdfReaderModule {}
