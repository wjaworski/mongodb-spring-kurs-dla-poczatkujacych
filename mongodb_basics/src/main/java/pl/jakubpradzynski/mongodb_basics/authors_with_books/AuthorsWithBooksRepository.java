package pl.jakubpradzynski.mongodb_basics.authors_with_books;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Repository;
import pl.jakubpradzynski.mongodb_basics.authors.Author;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Repository
public class AuthorsWithBooksRepository {
    private final MongoOperations mongoOperations;

    public AuthorsWithBooksRepository(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public List<AuthorsWithBooksViewModel> prepareAuthorsWithBooksViewModel(Instant now) {
        var joinAuthorsWithBooksStage = LookupOperation.newLookup().from("books").localField("_id").foreignField("authorIds").as("books");
        var calculateBooksCountStage = AddFieldsOperation.addField("booksCount")
                .withValueOf(ArrayOperators.arrayOf("books").length()).build();
        var calculateAgeStage = AddFieldsOperation.addField("age")
                .withValueOf(DateOperators.DateDiff.diffValueOf(ConditionalOperators.IfNull.ifNull("dateOfDeath").then(now), "year")
                        .toDateOf("dateOfBirth")).build();
        var combineNameWithSurnameStage = AddFieldsOperation.addField("author")
                .withValueOf(StringOperators.Concat.valueOf("name").concat(" ").concatValueOf("surname")).build();
        var sortAscendingByAuthorStage = new SortOperation(Sort.by(Sort.Direction.ASC, "author"));
        var projectToFinalViewStage = new ProjectionOperation()
                .andInclude("author", "booksCount", "dateOfBirth", "dateOfDeath", "age", "nationality", "books");
        return mongoOperations.aggregate(
                Aggregation.newAggregation(
                        joinAuthorsWithBooksStage,
                        calculateBooksCountStage,
                        calculateAgeStage,
                        combineNameWithSurnameStage,
                        sortAscendingByAuthorStage,
                        projectToFinalViewStage
                ),
                Author.class,
                AuthorsWithBooksViewModel.class
        ).getMappedResults();
    }

}
