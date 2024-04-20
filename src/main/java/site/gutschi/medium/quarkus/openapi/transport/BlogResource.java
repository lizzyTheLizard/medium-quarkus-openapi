package site.gutschi.medium.quarkus.openapi.transport;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.openapitools.api.BlogApi;
import org.openapitools.model.GenPost;
import org.openapitools.model.GenPostUpdate;
import org.openapitools.model.GenPostsInner;
import org.openapitools.model.GenSuccessResponse;

public class BlogResource implements BlogApi {

  @Override
  public Uni<GenSuccessResponse> createOrUpdatePost(String id, GenPostUpdate genPostUpdate) {
    return Uni.createFrom().failure(new NotAllowedException("Not allowed"));
  }

  @Override
  public Uni<GenSuccessResponse> deletePost(String id) {
    return Uni.createFrom().failure(new NotFoundException("Post not found"));
  }

  @Override
  public Uni<List<GenPostsInner>> getAllPosts(BigDecimal page) {
    return Uni.createFrom().item(List.of());
  }

  @Override
  public Uni<GenPost> getPost(String id) {
    return Uni.createFrom().failure(new NotFoundException("Post not found"));
  }
}
