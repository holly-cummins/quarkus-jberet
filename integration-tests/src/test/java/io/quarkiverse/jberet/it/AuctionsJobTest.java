package io.quarkiverse.jberet.it;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import javax.batch.runtime.BatchStatus;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkiverse.jberet.it.AuctionResource.JobData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.Header;

@QuarkusTest
class AuctionsJobTest {
    @BeforeAll
    static void beforeAll() {
        RestAssured.filters(
                (requestSpec, responseSpec, ctx) -> {
                    requestSpec.header(new Header(ACCEPT, APPLICATION_JSON));
                    return ctx.next(requestSpec, responseSpec);
                },
                new RequestLoggingFilter(),
                new ResponseLoggingFilter());

    }

    @AfterAll
    static void afterAll() {
        RestAssured.reset();
    }

    @Test
    void auctions() {
        JobData jobData = given()
                .get("/auctions/job/execute/auctions.json")
                .then()
                .statusCode(200)
                .extract().as(JobData.class);

        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobData current = given().get("/auctions/job/execution/{id}", jobData.getExecutionId())
                    .then()
                    .statusCode(200)
                    .extract().as(JobData.class);
            return BatchStatus.COMPLETED.equals(BatchStatus.valueOf(current.getStatus()));
        });

        Auction auction = given().get("/auctions/{id}", 279573567L).then().statusCode(200).extract().as(Auction.class);
        assertNotNull(auction);
        assertEquals(3800000, auction.getBid());
        assertEquals(4000000, auction.getBuyout());
        assertEquals(22792, auction.getItemId());
        assertEquals(20, auction.getQuantity());
    }
}
