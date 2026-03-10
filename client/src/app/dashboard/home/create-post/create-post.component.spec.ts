import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CreatePostComponent } from './create-post.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { HomePostService } from '../../../service/home/home-post.service';
import { FormsModule } from '@angular/forms';

describe('CreatePostComponent', () => {
  let component: CreatePostComponent;
  let fixture: ComponentFixture<CreatePostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreatePostComponent],
      imports: [HttpClientTestingModule, FormsModule],
      providers: [HomePostService]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreatePostComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should reset form on close', () => {
    component.newPostTitle = 'Test Title';
    component.newPostContent = 'Test Content';
    component.selectedFiles = [new File([''], 'test.jpg')];
    
    component.close();
    
    expect(component.newPostTitle).toBe('');
    expect(component.newPostContent).toBe('');
    expect(component.selectedFiles.length).toBe(0);
  });

  it('should show error when title is empty', () => {
    component.newPostTitle = '';
    component.newPostContent = 'Some content';
    
    component.createPost();
    
    expect(component.error).toBe('Title and content are required');
  });

  it('should show error when content is empty', () => {
    component.newPostTitle = 'Some title';
    component.newPostContent = '';
    
    component.createPost();
    
    expect(component.error).toBe('Title and content are required');
  });

  it('should remove file at index', () => {
    component.selectedFiles = [
      new File([''], 'test1.jpg'),
      new File([''], 'test2.jpg'),
      new File([''], 'test3.jpg')
    ];
    
    component.removeFile(1);
    
    expect(component.selectedFiles.length).toBe(2);
    expect(component.selectedFiles[0].name).toBe('test1.jpg');
    expect(component.selectedFiles[1].name).toBe('test3.jpg');
  });
});
