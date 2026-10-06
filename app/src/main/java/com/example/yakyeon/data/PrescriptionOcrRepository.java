package com.example.yakyeon.data;

import com.example.yakyeon.data.model.PrescriptionOcrResult;

import java.util.List;

public interface PrescriptionOcrRepository {

    List<PrescriptionOcrResult> recognize();
}
