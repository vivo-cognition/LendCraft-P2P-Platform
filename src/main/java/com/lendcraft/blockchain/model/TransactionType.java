package com.lendcraft.blockchain.model;

public enum TransactionType {
    REGULAR,        // Обычный перевод монет (Transfer)
    LOAN_REQUEST,   // Создание заявки (BORROW блокирует залог, LEND блокирует монеты займа)
    LOAN_CANCEL,    // Отмена заявки до старта (разблокировка средств создателю)
    LOAN_ACCEPT,    // Принятие заявки второй стороной (займ становится ACTIVE)
    LOAN_REPAY,     // Частичное или полное погашение долга
    LOAN_DEFAULT    // Срок истек, залог принудительно уходит кредитору
}
