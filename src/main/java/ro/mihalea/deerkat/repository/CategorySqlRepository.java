package ro.mihalea.deerkat.repository;

import lombok.extern.log4j.Log4j2;
import ro.mihalea.deerkat.exception.repository.*;
import ro.mihalea.deerkat.model.Category;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
public class CategorySqlRepository extends AbstractSqlRepository<Category>{
    /**
     * Initialise the repository and connect to the local repository at the default file path
     *
     * @throws RepositoryConnectionException Failed to connect to the repository
     */
    public CategorySqlRepository() throws RepositoryConnectionException {
        super();
    }

    @Override
    public Optional<Long> add(Category category) throws RepositoryCreateException {
        try {
            String createString = "INSERT INTO category (parentId, title) VALUES (?, ?)";

            PreparedStatement statement = connection.prepareStatement(createString, Statement.RETURN_GENERATED_KEYS);

            statement.setLong(1, category.getParentId());
            statement.setString(2, category.getTitle());

            statement.executeUpdate();

            log.debug("Category added to repository: " + category);

            return this.extractId(statement);
        } catch (SQLException e) {
            throw new RepositoryCreateException("Failed to add category to the database: " + category, e);
        }
    }

    @Override
    public List<Category> getAll() throws RepositoryReadException {
        List<Category> categories = new ArrayList<>();
        try {
            String queryString = "SELECT id, parentId, title FROM categories";
            Statement statement = connection.createStatement();

            ResultSet result = statement.executeQuery(queryString);

            while(result.next()) {
                Long id = result.getLong("id");
                Long parentId = result.getLong("parentId");
                // Null columns in the database return 0, so this if is required
                if(result.wasNull()) {
                    parentId = null;
                }
                String title = result.getString("title");


                Category category = Category.builder()
                        .id(id)
                        .parentId(parentId)
                        .title(title)
                        .build();

                if(category != null) {
                    categories.add(category);
                }
            }

            log.info("Database returned {} categories", result.getRow());
        } catch (SQLException e) {
            throw new RepositoryReadException("Failed to retrieve all categories", e);
        }

        return categories;
    }

    @Override
    public void nuke() throws RepositoryDeleteException {
        this.nukeTable("categories");
    }
}
