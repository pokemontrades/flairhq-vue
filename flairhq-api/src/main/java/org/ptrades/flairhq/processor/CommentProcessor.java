package org.ptrades.flairhq.processor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.ptrades.flairhq.dto.CommentRequest;
import org.ptrades.flairhq.dto.CommentResponse;
import org.ptrades.flairhq.mapper.CommentMapper;
import org.ptrades.flairhq.repository.CommentRepository;
import org.ptrades.flairhq.repository.domain.Comment;
import org.ptrades.flairhq.service.ModSecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommentProcessor {

    private final CommentRepository  commentRepository;
    private final CommentMapper      commentMapper;
    private final ModSecurityService modSecurity;

    public CommentProcessor(CommentRepository commentRepository, CommentMapper commentMapper,
                            ModSecurityService modSecurity) {
        this.commentRepository = commentRepository;
        this.commentMapper     = commentMapper;
        this.modSecurity       = modSecurity;
    }

    public List<CommentResponse> getComments(String user) {
        List<Comment> comments = (user != null)
                ? commentRepository.findByUser(user)
                : commentRepository.findAll();
        return comments.stream().map(commentMapper::toResponse).toList();
    }

    public Optional<CommentResponse> getComment(String id) {
        return commentRepository.findById(Objects.requireNonNull(id))
                .map(commentMapper::toResponse);
    }

    public CommentResponse addComment(CommentRequest request, String commenter) {
        Comment saved = commentRepository.save(
                Objects.requireNonNull(commentMapper.toDocument(request, commenter)));
        return commentMapper.toResponse(saved);
    }

    public void deleteComment(String id, String username, Authentication authentication) {
        Comment comment = commentRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!username.equals(comment.getUser2()) && !modSecurity.hasPermission(Objects.requireNonNull(authentication), "flair")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        commentRepository.deleteById(id);
    }
}
