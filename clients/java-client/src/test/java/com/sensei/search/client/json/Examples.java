package com.sensei.search.client.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;

import com.sensei.search.client.json.req.Facet;
import com.sensei.search.client.json.req.FacetInit;
import com.sensei.search.client.json.req.Operator;
import com.sensei.search.client.json.req.Selection;
import com.sensei.search.client.json.req.SenseiClientRequest;
import com.sensei.search.client.json.req.Sort;
import com.sensei.search.client.json.req.filter.Filters;
import com.sensei.search.client.json.req.query.Queries;
import com.sensei.search.client.json.req.query.Query;
import com.sensei.search.client.json.req.query.TextQuery.Type;
import com.sensei.search.client.json.res.SenseiResult;

public class Examples {
    public static void main(String[] args) throws Exception {
      //Equivalent to
      //SELECT color, year, tags, price FROM cars WHERE QUERY IS "cool" AND tags
      //CONTAINS ALL ("cool", "hybrid") EXCEPT ("favorite") AND color in ("red")
      //ORDER BY price desc LIMIT 0,10 BROWSE BY color(true, 1, 10, hits), year(true, 1, 10, value), price

      SenseiClientRequest senseiRequest = SenseiClientRequest.builder()
            .addFacet("color", Facet.builder().minHit(1).expand(true).orderByHits().max(10).build())
            //.addFacet("price", Facet.builder().minHit(1).expand(false).orderByHits().max(10).build())
            .addFacet("year", Facet.builder().minHit(1).expand(true).orderByVal().max(10).build())
            .query(Queries.stringQuery("cool"))
            .addSelection(Selection.terms("tags", Arrays.asList("cool", "hybrid"), Arrays.asList("favorite"), Operator.and))
            .addSelection(Selection.terms("color", Arrays.asList("red"), new ArrayList<String>(), Operator.or))
            .paging(10, 0)
            .fetchStored(true)
            .addSort(Sort.desc("price"))
        .build();
        JSONObject serialized = (JSONObject) JsonSerializer.serialize(senseiRequest);
        System.out.println(serialized.toString(2));
        SenseiResult senseiResult = new SenseiServiceProxy("localhost", "8080").sendSearchRequest(senseiRequest);
        System.out.println(senseiResult);
    }
    public static SenseiClientRequest.Builder basicWithSelections(SenseiClientRequest.Builder builder) {
        builder.paging(5, 2)
        .groupBy(7, "car", "year")
        .addSelection(Selection.path("field", "value", true, 1))
        .addSelection(Selection.range("color", "*", "*"))
        .addFacet("facet1", Facet.builder().max(2).minHit(1).orderByVal().build())
        .addFacetInit("name", "parameter", FacetInit.build("string", "val1", "val2"))
        .addSort(Sort.desc("color"))
        .addSort(Sort.asc("year"))
        .addTermVector("Term1")
        .explain(true)
        .partitions(Arrays.asList(1,2)) ;
        return builder;
    }
    public static SenseiClientRequest.Builder filters(SenseiClientRequest.Builder builder) {
        builder.filter(Filters.or(

                Filters.and(
                    Filters.boolMust(Filters.or(Filters.term("field", "value"))),
                    Filters.and(
                                Filters.or(Filters.term("field", "value"),
                                           Filters.terms("Field", Arrays.asList("a","b"), Arrays.asList("a","b"), Operator.or)
                                )
                    ),
                    Filters.ids(Arrays.asList("val2", "val3"), Arrays.asList("ExcludedValue")),
                    Filters.range("field1", "*", "*")),
                    Filters.or(Filters.ids(Arrays.asList("val2", "val3"), Arrays.asList("ExcludedValue")), Filters.range("field1", "*", "*", true, true)),
                   Filters.ids(Arrays.asList("val2", "val3"), Arrays.asList("ExcludedValue")),
                Filters.term("field", "value"),
                 Filters.boolShould(Filters.or(Filters.term("field", "value")))));
        return builder;
    }

	public static SenseiClientRequest.Builder queries(SenseiClientRequest.Builder builder) {
	    List<Query> innerQueries = Arrays.asList(
	                    Queries.matchAllQuery(3),
	                    Queries.disMax(2.0, 1.0, Queries.term("field1", "value1", 1.0)),
	                    Queries.ids(Arrays.asList("val2", "val3"), Arrays.asList("ExcludedValue"), 1.0),
	                    Queries.matchAllQuery(3.0),
	                    Queries.path("field", "/path1/path2", 1.0),
	                    Queries.prefix("field", "prefix", 2.0),
	                    Queries.wildcard("field", "wildcard", 3.0),
	                    Queries.spanFirst(Queries.spanTerm("field", "val", 3.5), 3, 1.0),
	                    Queries.spanNear(Arrays.asList(Queries.spanTerm("field", "val", 3.5)), 3, true, true, 1.0),
	                    Queries.spanNot(Queries.spanTerm("field", "val", 3.5), Queries.spanTerm("field", "val2", 3.5), 1.0),
	                    Queries.spanOr( 1.0, Queries.spanTerm("field", "val", 3.5)),
	                    Queries.spanTerm("field", "val", 3.5),
	                    Queries.textQuery("column","text", Operator.or, Type.phrase, 1.0),
	                            Queries.stringQueryBuilder().query("").autoGeneratePhraseQueries(true).defaultField("field").defaultOperator(Operator.and).fields("field1","field2").tieBreaker(2).build()
	              );
	    builder.query(Queries.bool(innerQueries, null, null, 2, 2.0, true));



        return builder;
    }


}