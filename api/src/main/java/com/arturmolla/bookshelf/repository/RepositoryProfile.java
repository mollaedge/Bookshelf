package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositoryProfile extends JpaRepository<User, Long> {
}
