@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.chabi.ui

import com.example.chabi.format.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.chabi.domain.*
import java.util.UUID

@Composable
fun VehicleExpenses(state: DriveUiState, vm: ChabiViewModel) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    Panel {
        PleosTextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().testTag("toggle-extra-costs")) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("통행료·주차비도 있나요?", style = MaterialTheme.typography.titleSmall)
                Text(if (state.draft.expenses.isEmpty()) "있을 때만 더해요" else "${state.draft.expenses.size}건 · ${won(state.additionalWon)}",
                    style = MaterialTheme.typography.labelMedium, color = Muted)
            }
            Text(if (expanded) "↑" else "↓")
        }
        if (expanded) {
            if (state.draft.expenses.any { it.category !in listOf(ExpenseCategory.TOLL, ExpenseCategory.PARKING) })
                Hint("통행료·주차비 외의 비용도 함께 계산하고 있어요.")
            state.draft.expenses.forEach { expense ->
                HorizontalDivider()
                MoneyRow(expense.title, won(expense.amountWon), strong = true)
                val participantIds = expense.participantIds
                if (participantIds == null) Hint("${state.draft.participants.size}명이 모두 나눠 내요")
                else if (participantIds.isEmpty()) Hint("나눠 낼 사람이 없어요. 수정에서 골라 주세요.", true)
                else Hint(state.draft.participants.filter { it.id in participantIds }.joinToString(", ") { it.name } + "만 나눠 내요")
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PleosOutlinedButton(onClick = { editingId = expense.id }, Modifier.testTag("edit-expense:${expense.id}")) { Text("수정") }
                    PleosTextButton(onClick = { vm.removeExpense(expense.id) }, Modifier.testTag("delete-expense:${expense.id}")) { Text("삭제", color = MaterialTheme.colorScheme.error) }
                }
            }
            PleosOutlinedButton(onClick = { adding = true }, modifier = Modifier.fillMaxWidth().testTag("add-expense")) { Text("통행료·주차비 추가") }
        }
    }
    val existing = state.draft.expenses.find { it.id == editingId }
    if (adding || existing != null) ExpenseEditor(existing, state.draft.participants,
        onDismiss = { adding = false; editingId = null },
        onSave = { vm.saveExpense(it); adding = false; editingId = null })
}

@Composable
private fun ExpenseEditor(existing: Expense?, people: List<Participant>, onDismiss: () -> Unit, onSave: (Expense) -> Unit) {
    val id = rememberSaveable(existing?.id) { existing?.id ?: UUID.randomUUID().toString() }
    var categoryName by rememberSaveable(id) { mutableStateOf((existing?.category ?: ExpenseCategory.TOLL).name) }
    var title by rememberSaveable(id) { mutableStateOf(existing?.title ?: "통행료") }
    var amount by rememberSaveable(id) { mutableStateOf(existing?.amountWon?.toString() ?: "") }
    var memo by rememberSaveable(id) { mutableStateOf(existing?.memo ?: "") }
    var everyone by rememberSaveable(id) { mutableStateOf(existing?.participantIds == null) }
    var selected by rememberSaveable(id, stateSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })) {
        mutableStateOf(existing?.participantIds ?: people.map { it.id }.toSet())
    }
    val numericAmount = amount.toLongOrNull()
    val amountInRange = numericAmount != null && numericAmount in 1..MAX_EXPENSE_WON
    val valid = title.isNotBlank() && amountInRange && (everyone || selected.isNotEmpty())
    PleosFormDialog(title = if (existing == null) "비용 추가" else "비용 수정", onDismiss = onDismiss,
        confirmEnabled = valid, confirmTag = "save-expense", onConfirm = { if (valid) onSave(Expense(id, ExpenseCategory.valueOf(categoryName), title.trim(),
            numericAmount!!, memo.trim(), if (everyone) null else selected)) }) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    (listOf(ExpenseCategory.TOLL, ExpenseCategory.PARKING) + listOfNotNull(existing?.category)).distinct().forEach { category -> NumberPill(category.title, categoryName == category.name) {
                        if (title.isBlank() || title == ExpenseCategory.valueOf(categoryName).title) title = category.title
                        categoryName = category.name
                    } }
                }
                PleosTextField(title, { title = it.take(30) }, Modifier.fillMaxWidth().testTag("expense-title"),
                    label = { Text("항목 이름") }, singleLine = true, isError = title.isBlank())
                PleosTextField(amount, { amount = it.take(12) }, Modifier.fillMaxWidth().testTag("expense-amount"),
                    label = { Text("금액") }, suffix = { Text("원") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amount.isNotEmpty() && !amountInRange)
                Hint("${won(MAX_EXPENSE_WON)}까지 입력할 수 있어요")
                PleosTextField(memo, { memo = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("메모 (선택)") }, maxLines = 2)
                Text("누가 나눠 내나요?", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberPill("모두", everyone) { everyone = true }
                    NumberPill("일부만", !everyone) { everyone = false }
                }
                if (!everyone) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        people.forEach { person -> NumberPill(person.name.ifBlank { "이름 미입력" }, person.id in selected) {
                            selected = if (person.id in selected) selected - person.id else selected + person.id
                        } }
                    }
                    if (selected.isEmpty()) Hint("한 명 이상 골라 주세요.", true)
                }
    }
}
