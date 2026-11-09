package com.example.yakyeon2.api;

public final class ApiMessages {

    private ApiMessages() {
    }

    public static String fallback(String code) {
        if (ApiCodes.INVALID_CREDENTIALS.equals(code)) {
            return "아이디 또는 비밀번호가 올바르지 않습니다.";
        }
        if (ApiCodes.LOGIN_LOCKED.equals(code)) {
            return "로그인 시도가 너무 많아요. 15분 뒤에 다시 시도해주세요.";
        }
        if (ApiCodes.CURRENT_PASSWORD_MISMATCH.equals(code)) {
            return "현재 비밀번호가 올바르지 않습니다.";
        }
        if (ApiCodes.PASSWORD_MISMATCH.equals(code)) {
            return "비밀번호와 비밀번호 확인이 일치하지 않습니다.";
        }
        if (ApiCodes.INVALID_REFRESH_TOKEN.equals(code)) {
            return "유효하지 않거나 만료된 토큰입니다. 다시 로그인해주세요.";
        }
        if (ApiCodes.REFRESH_TOKEN_REUSED.equals(code)) {
            return "비정상적인 접근이 감지되어 모든 로그인이 종료되었습니다. 다시 로그인해주세요.";
        }
        if (ApiCodes.UNAUTHENTICATED.equals(code)) {
            return "로그인이 만료되었습니다. 다시 로그인해 주세요.";
        }
        if (ApiCodes.USER_NOT_FOUND.equals(code)) {
            return "존재하지 않는 계정입니다.";
        }
        if (ApiCodes.ACCOUNT_NOT_FOUND.equals(code)) {
            return "입력하신 정보와 일치하는 계정이 없습니다.";
        }
        if (ApiCodes.RATE_LIMITED.equals(code)) {
            return "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (ApiCodes.INVALID_NOTIFICATION_SETTING.equals(code)) {
            return "알림 설정 요청이 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_PHONE_FORMAT.equals(code)) {
            return "휴대폰 번호 형식이 올바르지 않습니다.";
        }
        if (ApiCodes.VERIFICATION_RATE_LIMITED.equals(code)) {
            return "인증번호 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (ApiCodes.VERIFICATION_EXPIRED.equals(code)) {
            return "인증 시간이 만료되었습니다. 인증번호를 다시 요청해 주세요.";
        }
        if (ApiCodes.VERIFICATION_NOT_FOUND.equals(code)) {
            return "존재하지 않는 인증 요청입니다.";
        }
        if (ApiCodes.VERIFICATION_ALREADY_COMPLETED.equals(code)) {
            return "이미 인증이 완료된 요청입니다.";
        }
        if (ApiCodes.VERIFICATION_ATTEMPTS_EXCEEDED.equals(code)) {
            return "인증번호 입력 가능 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요.";
        }
        if (ApiCodes.REQUIRED_TERMS_NOT_AGREED.equals(code)) {
            return "필수 약관에 모두 동의해야 가입할 수 있습니다.";
        }
        if (ApiCodes.VERIFICATION_NOT_COMPLETED.equals(code)) {
            return "문자 인증이 완료되지 않았습니다.";
        }
        if (ApiCodes.DUPLICATE_LOGIN_ID.equals(code)) {
            return "이미 사용 중인 아이디입니다.";
        }
        if (ApiCodes.DUPLICATE_PHONE.equals(code)) {
            return "이미 가입된 휴대폰 번호입니다.";
        }
        if (ApiCodes.TERMS_VERSION_OUTDATED.equals(code)) {
            return "약관 내용이 갱신되었습니다. 최신 약관을 다시 확인해 주세요.";
        }
        if (ApiCodes.DEVICE_NOT_FOUND.equals(code)) {
            return "등록된 약통이 없습니다.";
        }
        if (ApiCodes.PATIENT_NOT_FOUND.equals(code)) {
            return "존재하지 않는 복약자입니다.";
        }
        if (ApiCodes.SHARE_NOT_GRANTED.equals(code)) {
            return "복약자가 공유를 허용하지 않은 정보입니다.";
        }
        if (ApiCodes.INSUFFICIENT_PERMISSION.equals(code)) {
            return "이 복약자의 데이터에 대해 해당 작업을 할 권한이 없습니다.";
        }
        if (ApiCodes.GUARDIAN_LINK_NOT_FOUND.equals(code)) {
            return "존재하지 않는 연동 요청입니다.";
        }
        if (ApiCodes.GUARDIAN_LINK_NOT_ACTIVE.equals(code)) {
            return "연동 중인 상태가 아닙니다.";
        }
        if (ApiCodes.CONSENT_REQUIRED.equals(code)) {
            return "필요한 동의가 없습니다.";
        }
        if (ApiCodes.INTERACTION_NOT_FOUND.equals(code)) {
            return "존재하지 않는 병용금기 정보입니다.";
        }
        if (ApiCodes.PRESCRIPTION_NOT_FOUND.equals(code)) {
            return "존재하지 않는 처방 정보입니다.";
        }
        if (ApiCodes.COMPARTMENT_MAPPING_NOT_FOUND.equals(code)) {
            return "존재하지 않는 칸 배정입니다.";
        }
        if (ApiCodes.DEVICE_ALREADY_REGISTERED.equals(code)) {
            return "이미 등록된 약통이 있습니다. 먼저 해제해 주세요.";
        }
        if (ApiCodes.DEVICE_SERIAL_IN_USE.equals(code)) {
            return "이미 다른 복약자에게 등록된 약통입니다.";
        }
        if (ApiCodes.COMPARTMENT_OCCUPIED.equals(code)) {
            return "이미 다른 약봉지가 배정된 칸입니다.";
        }
        if (ApiCodes.PRESCRIPTION_ALREADY_ASSIGNED.equals(code)) {
            return "이미 약통 칸에 배정된 약봉지입니다.";
        }
        if (ApiCodes.PRESCRIPTION_ALREADY_ENDED.equals(code)) {
            return "이미 복용이 끝난 약봉지입니다.";
        }
        if (ApiCodes.MAPPING_ALREADY_ENDED.equals(code)) {
            return "이미 해제된 칸 배정입니다.";
        }
        if (ApiCodes.MAPPING_NOT_PENDING_LOAD.equals(code)) {
            return "이미 충전이 확인된 칸입니다.";
        }
        if (ApiCodes.MAPPING_NOT_ACTIVE.equals(code)) {
            return "충전이 확인된 칸만 추가로 채울 수 있습니다.";
        }
        if (ApiCodes.INVALID_COMPARTMENT_NO.equals(code)) {
            return "이 약통에 없는 칸 번호입니다.";
        }
        if (ApiCodes.INVALID_MEDICATION.equals(code)) {
            return "약물 정보가 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_SCHEDULE.equals(code)) {
            return "복용 시각이 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_TAKEN_AT.equals(code)) {
            return "복용 시각이 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_QUERY.equals(code)) {
            return "조회 조건이 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_SEARCH_QUERY.equals(code)) {
            return "검색어가 올바르지 않습니다.";
        }
        if (ApiCodes.INVALID_IMAGE.equals(code)) {
            return "올바른 이미지 파일이 아닙니다. JPEG 또는 PNG 이미지를 올려주세요.";
        }
        if (ApiCodes.DOSE_NOT_FOUND.equals(code)) {
            return "존재하지 않는 복용 예정입니다.";
        }
        if (ApiCodes.DOSE_EXPIRED.equals(code)) {
            return "당일 기록만 바꿀 수 있습니다. 지난 날의 복용 기록은 수정할 수 없습니다.";
        }
        if (ApiCodes.DOSE_NOT_DECIDED.equals(code)) {
            return "아직 기록이 없는 복용 예정입니다.";
        }
        if (ApiCodes.DOSE_RECORD_FAILED.equals(code)) {
            return "복용 기록을 처리하지 못했습니다. 잠시 후 다시 시도해주세요.";
        }
        if (ApiCodes.MEDICATION_NOT_FOUND.equals(code)) {
            return "존재하지 않는 약물 정보입니다.";
        }
        if (ApiCodes.MEDICATION_ALREADY_ENDED.equals(code)) {
            return "이미 복용이 끝난 약입니다.";
        }
        if (ApiCodes.MEDICATION_NOT_STARTED.equals(code)) {
            return "아직 복용을 시작하지 않은 약은 종료할 수 없습니다. 삭제해주세요.";
        }
        if (ApiCodes.DRUG_NOT_FOUND.equals(code)) {
            return "존재하지 않는 약물입니다.";
        }
        if (ApiCodes.OCR_UNAVAILABLE.equals(code)) {
            return "처방전 인식을 사용할 수 없습니다.";
        }
        if (ApiCodes.SCHEDULE_CONFLICT.equals(code)) {
            return "복용 시각이 서로 겹칩니다.";
        }
        return null;
    }

    public static boolean prefersServerText(String code) {
        return ApiCodes.VALIDATION_ERROR.equals(code) || ApiCodes.INVALID_SCHEDULE.equals(code);
    }

    public static String deviceRegister(int status, String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        if (status == 400 && ApiCodes.DEVICE_CREDENTIALS_INVALID.equals(code)) {
            return "시리얼번호와 비밀값을 다시 확인해 주세요. 약통 하단 라벨을 확인해 보세요.";
        }
        if (status == 409 && ApiCodes.DEVICE_ALREADY_REGISTERED.equals(code)) {
            return "이미 등록된 약통이 있습니다. 기존 약통을 확인해 주세요.";
        }
        if (status == 409 && ApiCodes.DEVICE_SERIAL_IN_USE.equals(code)) {
            return "다른 복약자가 사용 중인 약통입니다.";
        }
        if (status == 403 && ApiCodes.CONSENT_REQUIRED.equals(code)) {
            return "약통 연결을 위한 동의가 필요합니다.";
        }
        if (status == 429 && ApiCodes.RATE_LIMITED.equals(code)) {
            return "약통 등록 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.";
        }
        return null;
    }
}
