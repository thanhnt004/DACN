package com.example.backend.dto.response.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchResult<T>{
    private List<T> successItems = new ArrayList<>();;
    private List<FailureItem<T>> failedItems= new ArrayList<>();;

    public void addSuccess(T orderId) {
        this.successItems.add(orderId);
    }

    public void addFailure(T orderId, String message) {
        this.failedItems.add(new FailureItem<>(orderId, message));
    }

    public boolean hasFailures() {
        return !this.failedItems.isEmpty();
    }

    public static class FailureItem<T> {
        private T item;
        @Getter
        private String reason;

        public FailureItem(T item, String reason) {
            this.item = item;
            this.reason = reason;
        }

        public T getId() { return item; }
    }
}
