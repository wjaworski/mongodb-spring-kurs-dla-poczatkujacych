package pl.jakubpradzynski.mongodb_basics.authors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface AuthorsRepository {
    void save(Author author);

    Optional<Author> findById(ObjectId authorId);

    void deleteById(ObjectId authorId);

    void deleteAll();

    List<Author> findByNationalityOrLiving(String nationality, boolean living);

    List<AuthorWithAge> calculateAuthorAges(Instant now);
}

@Repository
interface SpringAuthorsRepository extends MongoRepository<Author, ObjectId> {

}

@Repository
class AuthorsRepositoryImpl implements AuthorsRepository {
    private final SpringAuthorsRepository springAuthorsRepository;
    private final MongoOperations mongoOperations;

    AuthorsRepositoryImpl(SpringAuthorsRepository springAuthorsRepository, MongoOperations mongoOperations) {
        this.springAuthorsRepository = springAuthorsRepository;
        this.mongoOperations = mongoOperations;
    }

    @Override
    public void save(Author author) {
        mongoOperations.save(author);
    }

    @Override
    public Optional<Author> findById(ObjectId authorId) {
        return springAuthorsRepository.findById(authorId);
    }

    @Override
    public void deleteById(ObjectId authorId) {
        springAuthorsRepository.deleteById(authorId);
    }

    @Override
    public void deleteAll() {
        springAuthorsRepository.deleteAll();
    }


    @Override
    public List<Author> findByNationalityOrLiving(String nationality, boolean living) {
        Criteria dateOfDeathCriteria = living
                ? Criteria.where("dateOfDeath").exists(false)
                : Criteria.where("dateOfDeath").exists(true);
        return mongoOperations.find(
                Query.query(new Criteria().orOperator(
                        Criteria.where("nationality").is(nationality),
                        dateOfDeathCriteria
                )).with(Sort.by(Sort.Direction.ASC, "dateOfBirth")),
                Author.class
        );
    }


    @Override
    public List<AuthorWithAge> calculateAuthorAges(Instant now) {
        var ageCalculator = DateOperators.DateDiff
                .diffValueOf(ConditionalOperators.IfNull.ifNull("dateOfDeath").then(now), "year")
                .toDateOf("dateOfBirth");
        return mongoOperations.aggregate(
                Aggregation.newAggregation(
                        Aggregation
                                .project("name", "surname")
                                .and(ageCalculator)
                                .as("age"),
                        Aggregation.sort(Sort.Direction.DESC, "age")
                ),
                Author.class,
                AuthorWithAge.class
        ).getMappedResults();
    }

}
