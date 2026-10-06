package com.example.yakyeon.data.mock;

import com.example.yakyeon.data.DurInteractionRepository;
import com.example.yakyeon.data.model.DurInteraction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MockDurInteractionRepository implements DurInteractionRepository {

    private static MockDurInteractionRepository instance;

    private final List<DurInteraction> interactions = Collections.unmodifiableList(Arrays.asList(
            new DurInteraction(
                    "dur-001",
                    "아스피린",
                    "와파린",
                    "항혈소판제",
                    "항응고제",
                    "병용금기 등재 안내",
                    "출혈 위험 증가",
                    "DUR에 병용금기로 등재된 조합입니다. 함께 복용 시 출혈 위험이 증가할 수 있습니다.",
                    "복용 중인 조합을 임의로 바꾸지 말고 의료진과 상담하세요.",
                    "주의"
            ),
            new DurInteraction(
                    "dur-002",
                    "메트포르민정 500mg",
                    "오메프라졸캡슐 20mg",
                    "혈당강하제",
                    "위장약",
                    "병용금기 등재 안내",
                    "함께 복용 시 주의",
                    "화면 테스트용 등재 예시입니다. 함께 복용할 때 주의가 필요한 조합으로 표시됩니다.",
                    "이 안내는 실제 판정이 아닙니다. 복용 변경 전 의료진과 상담하세요.",
                    "주의"
            ),
            new DurInteraction(
                    "dur-003",
                    "메트포르민정 500mg",
                    "요오드 조영제",
                    "혈당강하제",
                    "검사·시술 시 사용될 수 있음",
                    "병용금기 등재 안내",
                    "검사 계획 확인",
                    "두 성분의 병용금기 정보가 등재 예시로 표시되어 있습니다. 현재 복용 및 검사 계획을 의료진에게 알려 주세요.",
                    "검사나 시술 일정이 있으면 복용 중인 약물을 의료진에게 알리세요.",
                    "주의"
            )
    ));

    public static MockDurInteractionRepository getInstance() {
        if (instance == null) {
            instance = new MockDurInteractionRepository();
        }
        return instance;
    }

    @Override
    public List<DurInteraction> getInteractions() {
        return interactions;
    }

    @Override
    public DurInteraction findById(String id) {
        if (id == null) {
            return null;
        }
        for (DurInteraction interaction : interactions) {
            if (interaction.getId().equals(id)) {
                return interaction;
            }
        }
        return null;
    }
}
