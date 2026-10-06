package com.example.yakyeon.data;

import com.example.yakyeon.data.model.DurInteraction;

import java.util.List;

public interface DurInteractionRepository {

    List<DurInteraction> getInteractions();

    DurInteraction findById(String id);
}
