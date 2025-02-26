package pl.jakubpradzynski.mongodb_basics.books;


import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface BooksRepository {

    void save(Book book);

    Optional<Book> findById(ObjectId bookId);

    void deleteById(ObjectId bookId);

    void deleteAll();

    void changeScores(ObjectId bookId, Double goodreads, Double lubimyczytac);

    void addAuthor(ObjectId bookId, ObjectId additionalAuthorId);

    List<Book> findByGenre(Genre genre);

    int countByGenre(Genre genre);

    List<Book> findByScoresBiggerThen(double goodreadsScoreThreshold, double lubimyczytacScoreThreshold);

    List<BooksGroupedByPublisher> findAllGroupedByPublisher();

    List<Book> findByTextInDescription(String text);
}

@Repository
interface SpringBooksRepository extends MongoRepository<Book, ObjectId> {
}

@Repository
class BooksRepositoryImpl implements BooksRepository {
    private final SpringBooksRepository springBooksRepository;
    private final MongoOperations mongoOperations;

    public BooksRepositoryImpl(SpringBooksRepository springBooksRepository, MongoOperations mongoOperations) {
        this.springBooksRepository = springBooksRepository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void save(Book book) {
        springBooksRepository.save(book);
    }

    @Override
    public Optional<Book> findById(ObjectId bookId) {
        return springBooksRepository.findById(bookId);
    }

    @Override
    public void deleteById(ObjectId bookId) {
        springBooksRepository.deleteById(bookId);
    }

    @Override
    public void deleteAll() {
        springBooksRepository.deleteAll();
    }

    @Override
    public void changeScores(ObjectId bookId, Double goodreads, Double lubimyczytac) {
        mongoOperations.updateFirst(
                Query.query(Criteria.where("_id").is(bookId)),
                Update.update("score.goodreads", goodreads).set("score.lubimyczytac", lubimyczytac),
                Book.class
        );
    }

    @Override
    public void addAuthor(ObjectId bookId, ObjectId additionalAuthorId) {
        mongoOperations.updateFirst(
                Query.query(Criteria.where("_id").is(bookId)),
                new Update().addToSet("authorIds", additionalAuthorId),
                Book.class
        );
    }

    @Override
    public List<Book> findByGenre(Genre genre) {
        return mongoOperations.find(
                Query.query(Criteria.where("genres").in(genre)),
                Book.class
        );
    }

    @Override
    public int countByGenre(Genre genre) {
        return (int) mongoOperations.count(
                Query.query(Criteria.where("genres").in(genre)),
                Book.class
        );
    }

    @Override
    public List<Book> findByScoresBiggerThen(double goodreadsScoreThreshold, double lubimyczytacScoreThreshold) {
        return mongoOperations.find(
                Query.query(new Criteria().andOperator(
                        Criteria.where("score.goodreads").gt(goodreadsScoreThreshold),
                        Criteria.where("score.lubimyczytac").gt(lubimyczytacScoreThreshold)
                )).with(Sort.by(Sort.Direction.ASC, "score.goodreads", "score.lubimyczytac")),
                Book.class
        );
    }

    @Override
    public List<BooksGroupedByPublisher> findAllGroupedByPublisher() {
        return mongoOperations
                .aggregate(Aggregation.newAggregation(
                        Aggregation.group("publisher")
                                .addToSet("$$ROOT").as("books")
                                .count().as("count"),
                        Aggregation.sort(Sort.Direction.ASC, "_id")
                ), Book.class, BooksGroupedByPublisher.class)
                .getMappedResults();
    }

    @Override
    public List<Book> findByTextInDescription(String text) {
        return mongoOperations.find(
                TextQuery.queryText(TextCriteria.forDefaultLanguage().matching(text)),
                Book.class
        );

    }
}

