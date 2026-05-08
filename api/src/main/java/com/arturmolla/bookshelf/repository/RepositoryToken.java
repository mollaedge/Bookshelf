package com.arturmolla.bookshelf.repository;

import com.arturmolla.bookshelf.model.user.Token;
import com.arturmolla.bookshelf.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryToken extends JpaRepository<Token, Long> {

    Optional<Token> findByToken(String token);

    Optional<Token> findByTokenAndUserEmail(String token, String email);

    List<Token> findAllByUser(User user);
}
