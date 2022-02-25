package stream.api;

import common.test.tool.annotation.Difficult;
import common.test.tool.annotation.Easy;
import common.test.tool.dataset.ClassicOnlineStore;
import common.test.tool.entity.Customer;
import common.test.tool.util.CollectorImpl;

import org.junit.Test;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class Exercise9Test extends ClassicOnlineStore {

    @Easy @Test
    public void simplestStringJoin() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a String with comma separated names shown in the assertion.
         * The collector will be used by serial stream.
         */
        Supplier<StringJoiner> supplier = () -> new StringJoiner(",","","");
        BiConsumer<StringJoiner, String> accumulator = (StringJoiner,string)->StringJoiner.add(string);
        BinaryOperator<StringJoiner> combiner = null;
                //(StringJoiner1,StringJoiner2)-> StringJoiner1.merge(StringJoiner2);
        Function<StringJoiner, String> finisher = StringJoiner->StringJoiner.toString();

        Collector<String, ?, String> toCsv =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());
        String nameAsCsv = customerList.stream().map(Customer::getName).collect(toCsv);
        assertThat(nameAsCsv, is("Joe,Steven,Patrick,Diana,Chris,Kathy,Alice,Andrew,Martin,Amy"));
    }

    @Difficult @Test
    public void mapKeyedByItems() {
        List<Customer> customerList = this.mall.getCustomerList();

        /**
         * Implement a {@link Collector} which can create a {@link Map} with keys as item and
         * values as {@link Set} of customers who are wanting to buy that item.
         * The collector will be used by parallel stream.
         */
        Supplier<Map<String,Set<String>>> supplier = () -> new HashMap<>();

        BiConsumer<Map<String, Set<String>>, Customer> accumulator = ((stringSetMap, customer) ->
                customer.getWantToBuy().
                        forEach(item -> stringSetMap.merge(item.getName(), Stream.of(customer.getName()).collect(Collectors.toSet()), (existingSet, newCustomer) -> {
                            existingSet.addAll(newCustomer);
                            return existingSet;
                        })));

        BinaryOperator<Map<String, Set<String>>> combiner = (map1, map2) -> {
            map2.forEach((key, value) ->
                    map1.merge(key, value, (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    })
            );
            return map1;
        };

        Function<Map<String, Set<String>>, Map<String, Set<String>>> finisher = null;

        Collector<Customer, ?, Map<String, Set<String>>> toItemAsKey =
            new CollectorImpl<>(supplier, accumulator, combiner, finisher, EnumSet.of(
                Collector.Characteristics.CONCURRENT,
                Collector.Characteristics.IDENTITY_FINISH));
        Map<String, Set<String>> itemMap = customerList.stream().parallel().collect(toItemAsKey);
        assertThat(itemMap.get("plane"), containsInAnyOrder("Chris"));
        assertThat(itemMap.get("onion"), containsInAnyOrder("Patrick", "Amy"));
        assertThat(itemMap.get("ice cream"), containsInAnyOrder("Patrick", "Steven"));
        assertThat(itemMap.get("earphone"), containsInAnyOrder("Steven"));
        assertThat(itemMap.get("plate"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("fork"), containsInAnyOrder("Joe", "Martin"));
        assertThat(itemMap.get("cable"), containsInAnyOrder("Diana", "Steven"));
        assertThat(itemMap.get("desk"), containsInAnyOrder("Alice"));
    }

    @Difficult @Test
    public void bitList2BitString() {
        String bitList = "22-24,9,42-44,11,4,46,14-17,5,2,38-40,33,50,48";

        /**
         * Create a {@link String} of "n"th bit ON.
         * for example
         * "3" will be "001"
         * "1,3,5" will be "10101"
         * "1-3" will be "111"
         * "7,1-3,5" will be "1110101"
         */

        Supplier<List<Integer>> supplier = () -> new ArrayList<>();


        BiConsumer<List<Integer>, String> accumulator = (integerList, string) -> {
            if (string.contains("-")){
                List<String> splittedString = List.of(string.split("-"));
                int i = Integer.parseInt(splittedString.get(1)); //[22, 24]
                while (i>=Integer.parseInt(splittedString.get(0))){ // [24, 23, 22, 9]
                    integerList.add(i);
                    i--;
                }
            }else {
                integerList.add(Integer.valueOf(string));
            }
        };
        BinaryOperator<List<Integer>> combiner = null;
//                (list1, list2) -> {
//            list1.addAll(list2);
//            return list1;
//        };

        Function<List<Integer>, String> finisher = integerList -> {
            Optional<Integer> max = integerList.stream().max(Comparator.naturalOrder());
            List<String> finalResult = new ArrayList<>();
            int i = max.get();
            while (i>0){
                finalResult.add("0");
                i--;
            }

            // [0,0,0,0,0,0,0,0,0,0,0,0]
            // [24, 23, 22, 9]
            integerList.stream().forEach(integer -> finalResult.set(integer-1, "1"));
            return finalResult.stream().collect(Collectors.joining());
        };
        Collector<String, ?, String> toBitString = new CollectorImpl<>(supplier, accumulator, combiner, finisher, Collections.emptySet());


        String bitString = Arrays.stream(bitList.split(",")).collect(toBitString);
        assertThat(bitString, is("01011000101001111000011100000000100001110111010101")

        );
    }
}
