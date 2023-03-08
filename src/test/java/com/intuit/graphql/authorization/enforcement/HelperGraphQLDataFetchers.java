package com.intuit.graphql.authorization.enforcement;


import com.google.common.collect.ImmutableMap;
import graphql.schema.DataFetcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class HelperGraphQLDataFetchers {

  private static List<Map<String, Object>> books = new ArrayList<>(
      Arrays.asList(ImmutableMap.of("id", "book-1",
          "name", "Harry Potter and the Philosopher's Stone",
          "pageCount", "223",
          "authorId", "author-1"),
          ImmutableMap.of("id", "book-2",
              "name", "Moby Dick",
              "pageCount", "635",
              "authorId", "author-2"),
          ImmutableMap.of("id", "book-3",
              "name", "Interview with the vampire",
              "pageCount", "371",
              "authorId", "author-3")));

  private static List<Map<String, Object>> authors = new ArrayList<>(
      Arrays.asList(ImmutableMap.of("id", "author-1",
          "firstName", "Joanne",
          "lastName", "Rowling"),
          ImmutableMap.of("id", "author-2",
              "firstName", "Herman",
              "lastName", "Melville"),
          ImmutableMap.of("id", "author-3",
              "firstName", "Anne",
              "lastName", "Rice")
      ));

  private static List<Map<String, Object>> ratings = Arrays.asList(
      ImmutableMap.of("id", "book-1",
          "comments", "Good",
          "stars", "4"),
      ImmutableMap.of("id", "book-2",
          "comments", "Excellent",
          "stars", "5"),
      ImmutableMap.of("id", "book-3",
          "comments", "OK",
          "stars", "3")
  );

  public DataFetcher getBookByIdDataFetcher() {
    return dataFetchingEnvironment -> {
      String bookId = dataFetchingEnvironment.getArgument("id");
      return books
          .stream()
          .filter(book -> book.get("id").equals(bookId))
          .findFirst()
          .orElse(null);
    };
  }

  public DataFetcher allBooksDataFetcher() {
    return dataFetchingEnvironment -> books;
  }

  public DataFetcher getAuthorDataFetcher() {
    return dataFetchingEnvironment -> {
      Map<String, String> book = dataFetchingEnvironment.getSource();
      String authorId = book.get("authorId");
      return authors
          .stream()
          .filter(author -> author.get("id").equals(authorId))
          .findFirst()
          .orElse(null);
    };
  }

  public DataFetcher getRatingDataFetcher() {
    return dataFetchingEnvironment -> {
      Map<String, String> book = dataFetchingEnvironment.getSource();
      String bookId = book.get("id");
      return ratings
          .stream()
          .filter(rating -> rating.get("id").equals(bookId))
          .findFirst()
          .orElse(null);
    };
  }

  public DataFetcher addBookDataFetcher() {
    return dataFetchingEnvironment -> {
      Map<String, Object> bookInput = dataFetchingEnvironment.getArgument("input");
      String bookId = bookInput.get("id").toString();
      Map authorInfo = (Map) bookInput.get("author");

      books.add(ImmutableMap.of("id", bookId,
          "name", bookInput.get("name").toString(),
          "pageCount", bookInput.get("pageCount").toString(),
          "authorId", authorInfo.get("id").toString()));

      authors.add(ImmutableMap.of("id", authorInfo.get("id").toString(),
          "firstName", authorInfo.get("firstName").toString(),
          "lastName", authorInfo.get("lastName").toString()));

      return books
          .stream()
          .filter(book1 -> book1.get("id").equals(bookId))
          .findFirst()
          .orElse(null);
    };
  }

  public DataFetcher updateBookDataFetcher() {
    return dataFetchingEnvironment -> {
      Map<String, String> book = dataFetchingEnvironment.getArgument("input");
      String bookId = book.get("id");
      books.add(ImmutableMap.of("id", bookId,
          "name", book.get("name"),
          "pageCount", book.get("pageCount")));
      return books
          .stream()
          .filter(book1 -> book1.get("id").equals(bookId))
          .findFirst()
          .orElse(null);
    };
  }

  public DataFetcher removeBookDataFetcher() {
    return dataFetchingEnvironment -> {
      Map<String, String> book = dataFetchingEnvironment.getArgument("input");
      String bookId = book.get("id");
      Map<String, Object> b = books
          .stream()
          .filter(book1 -> book1.get("id").equals(bookId))
          .findFirst()
          .orElse(null);

      if (b != null) {
        books.remove(b);
      }
      return book;
    };
  }
}
