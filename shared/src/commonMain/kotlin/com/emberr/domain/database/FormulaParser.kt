package com.emberr.domain.database

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_formula_ends_too_early
import emberr.shared.generated.resources.message_formula_ifs_needs_pairs
import emberr.shared.generated.resources.message_formula_missing_closing_quote
import emberr.shared.generated.resources.message_formula_missing_symbol
import emberr.shared.generated.resources.message_formula_needs_at_least_values
import emberr.shared.generated.resources.message_formula_needs_either_values
import emberr.shared.generated.resources.message_formula_needs_no_values
import emberr.shared.generated.resources.message_formula_needs_values
import emberr.shared.generated.resources.message_formula_not_a_number
import emberr.shared.generated.resources.message_formula_piece_number
import emberr.shared.generated.resources.message_formula_piece_symbol
import emberr.shared.generated.resources.message_formula_piece_text
import emberr.shared.generated.resources.message_formula_piece_word
import emberr.shared.generated.resources.message_formula_prop_needs_name
import emberr.shared.generated.resources.message_formula_unexpected
import emberr.shared.generated.resources.message_formula_unknown_function
import emberr.shared.generated.resources.message_formula_unknown_word
import org.jetbrains.compose.resources.StringResource

class FormulaException(val problem: FormulaMessage) : Exception(problem.toString())

internal fun formulaPhrase(resource: StringResource, vararg arguments: Any): FormulaMessage.Phrase =
    FormulaMessage.Phrase(resource, arguments.toList())

enum class FormulaOperator(val symbol: String) {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/"),
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    GREATER(">"),
    LESS("<")
}

enum class FormulaFunction(val formulaName: String, val argumentCounts: IntRange) {
    IF("if", 3..3),
    AND("and", 1..Int.MAX_VALUE),
    OR("or", 1..Int.MAX_VALUE),
    NOT("not", 1..1),
    CONCAT("concat", 1..Int.MAX_VALUE),
    LENGTH("length", 1..1),
    ROUND("round", 1..2),
    ABS("abs", 1..1),
    MIN("min", 1..Int.MAX_VALUE),
    MAX("max", 1..Int.MAX_VALUE),
    EMPTY("empty", 1..1),
    NOW("now", 0..0),
    TODAY("today", 0..0),
    DATE_ADD("dateAdd", 3..3),
    DATE_BETWEEN("dateBetween", 3..3),
    FORMAT_DATE("formatDate", 2..2),
    YEAR("year", 1..1),
    MONTH("month", 1..1),
    DAY("day", 1..1),
    CONTAINS("contains", 2..2),
    FLOOR("floor", 1..1),
    CEIL("ceil", 1..1),
    MOD("mod", 2..2),
    IFS("ifs", 3..Int.MAX_VALUE);

    val argumentCountProblem: FormulaMessage
        get() = when {
            argumentCounts.last == 0 -> formulaPhrase(Res.string.message_formula_needs_no_values, formulaName)
            argumentCounts.last == Int.MAX_VALUE -> FormulaMessage.Counted(
                Res.plurals.message_formula_needs_at_least_values,
                argumentCounts.first,
                listOf(formulaName, argumentCounts.first)
            )
            argumentCounts.first == argumentCounts.last -> FormulaMessage.Counted(
                Res.plurals.message_formula_needs_values,
                argumentCounts.first,
                listOf(formulaName, argumentCounts.first)
            )
            else -> formulaPhrase(
                Res.string.message_formula_needs_either_values,
                formulaName,
                argumentCounts.first,
                argumentCounts.last
            )
        }
}

sealed class FormulaExpression {
    data class Literal(val value: FormulaValue) : FormulaExpression()
    data class PropertyReference(val propertyName: String) : FormulaExpression()
    data class Negation(val operand: FormulaExpression) : FormulaExpression()
    data class Operation(val operator: FormulaOperator, val left: FormulaExpression, val right: FormulaExpression) : FormulaExpression()
    data class FunctionCall(val function: FormulaFunction, val arguments: List<FormulaExpression>) : FormulaExpression()
}

fun FormulaExpression.propertyNames(): List<String> = when (this) {
    is FormulaExpression.Literal -> emptyList()
    is FormulaExpression.PropertyReference -> listOf(propertyName)
    is FormulaExpression.Negation -> operand.propertyNames()
    is FormulaExpression.Operation -> left.propertyNames() + right.propertyNames()
    is FormulaExpression.FunctionCall -> arguments.flatMap { it.propertyNames() }
}

fun parseFormula(text: String): FormulaExpression {
    if (text.isBlank()) return FormulaExpression.Literal(FormulaValue.Empty)
    return FormulaParser(tokensOf(text)).parseWholeFormula()
}

fun formulaTextLiteral(text: String): String = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

private sealed class FormulaToken {
    data class NumberToken(val number: Double) : FormulaToken()
    data class TextToken(val text: String) : FormulaToken()
    data class NameToken(val name: String) : FormulaToken()
    data class SymbolToken(val symbol: String) : FormulaToken()
}

private val twoCharacterSymbols = listOf("==", "!=", ">=", "<=")
private const val SINGLE_CHARACTER_SYMBOLS = "+-*/<>(),"

private fun tokensOf(text: String): List<FormulaToken> {
    val tokens = mutableListOf<FormulaToken>()
    var index = 0
    while (index < text.length) {
        val character = text[index]
        val twoCharacterSymbol = twoCharacterSymbols.firstOrNull { text.startsWith(it, index) }
        when {
            character.isWhitespace() -> index++
            character.isDigit() || (character == '.' && text.getOrNull(index + 1)?.isDigit() == true) -> {
                val start = index
                while (index < text.length && (text[index].isDigit() || text[index] == '.')) index++
                val numberText = text.substring(start, index)
                val number = numberText.toDoubleOrNull() ?: throw FormulaException(formulaPhrase(Res.string.message_formula_not_a_number, numberText))
                tokens += FormulaToken.NumberToken(number)
            }
            character == '"' -> {
                val quotedText = StringBuilder()
                index++
                while (true) {
                    if (index >= text.length) throw FormulaException(formulaPhrase(Res.string.message_formula_missing_closing_quote))
                    val quotedCharacter = text[index]
                    if (quotedCharacter == '"') {
                        index++
                        break
                    }
                    if (quotedCharacter == '\\' && index + 1 < text.length) {
                        quotedText.append(text[index + 1])
                        index += 2
                    } else {
                        quotedText.append(quotedCharacter)
                        index++
                    }
                }
                tokens += FormulaToken.TextToken(quotedText.toString())
            }
            character.isLetter() || character == '_' -> {
                val start = index
                while (index < text.length && (text[index].isLetterOrDigit() || text[index] == '_')) index++
                tokens += FormulaToken.NameToken(text.substring(start, index))
            }
            twoCharacterSymbol != null -> {
                tokens += FormulaToken.SymbolToken(twoCharacterSymbol)
                index += 2
            }
            character in SINGLE_CHARACTER_SYMBOLS -> {
                tokens += FormulaToken.SymbolToken(character.toString())
                index++
            }
            else -> throw FormulaException(
                formulaPhrase(Res.string.message_formula_unexpected, formulaPhrase(Res.string.message_formula_piece_symbol, character.toString()))
            )
        }
    }
    return tokens
}

private val comparisonOperators = listOf(
    FormulaOperator.EQUALS,
    FormulaOperator.NOT_EQUALS,
    FormulaOperator.GREATER_OR_EQUAL,
    FormulaOperator.LESS_OR_EQUAL,
    FormulaOperator.GREATER,
    FormulaOperator.LESS
)
private val addingOperators = listOf(FormulaOperator.PLUS, FormulaOperator.MINUS)
private val multiplyingOperators = listOf(FormulaOperator.TIMES, FormulaOperator.DIVIDE)

private class FormulaParser(private val tokens: List<FormulaToken>) {
    private var position = 0

    fun parseWholeFormula(): FormulaExpression {
        val expression = parseComparison()
        val leftoverToken = tokens.getOrNull(position)
        if (leftoverToken != null) throw FormulaException(formulaPhrase(Res.string.message_formula_unexpected, describe(leftoverToken)))
        return expression
    }

    private fun parseComparison(): FormulaExpression = parseOperations(comparisonOperators, ::parseAdding)

    private fun parseAdding(): FormulaExpression = parseOperations(addingOperators, ::parseMultiplying)

    private fun parseMultiplying(): FormulaExpression = parseOperations(multiplyingOperators, ::parseNegation)

    private fun parseOperations(operators: List<FormulaOperator>, parseOperand: () -> FormulaExpression): FormulaExpression {
        var expression = parseOperand()
        while (true) {
            val operator = takeOperator(operators) ?: return expression
            expression = FormulaExpression.Operation(operator, expression, parseOperand())
        }
    }

    private fun parseNegation(): FormulaExpression =
        if (takeSymbol("-")) FormulaExpression.Negation(parseNegation()) else parseSingleValue()

    private fun parseSingleValue(): FormulaExpression {
        val token = tokens.getOrNull(position) ?: throw FormulaException(formulaPhrase(Res.string.message_formula_ends_too_early))
        position++
        return when (token) {
            is FormulaToken.NumberToken -> FormulaExpression.Literal(FormulaValue.NumberValue(token.number))
            is FormulaToken.TextToken -> FormulaExpression.Literal(FormulaValue.TextValue(token.text))
            is FormulaToken.NameToken -> parseName(token.name)
            is FormulaToken.SymbolToken -> {
                if (token.symbol != "(") throw FormulaException(formulaPhrase(Res.string.message_formula_unexpected, describe(token)))
                val expressionInBrackets = parseComparison()
                expectSymbol(")")
                expressionInBrackets
            }
        }
    }

    private fun parseName(name: String): FormulaExpression {
        if (name.equals("true", ignoreCase = true)) return FormulaExpression.Literal(FormulaValue.BooleanValue(true))
        if (name.equals("false", ignoreCase = true)) return FormulaExpression.Literal(FormulaValue.BooleanValue(false))
        if (!takeSymbol("(")) throw FormulaException(formulaPhrase(Res.string.message_formula_unknown_word, name))
        val arguments = parseArguments()

        if (name.equals("prop", ignoreCase = true)) {
            val propertyName = ((arguments.singleOrNull() as? FormulaExpression.Literal)?.value as? FormulaValue.TextValue)?.text
                ?: throw FormulaException(formulaPhrase(Res.string.message_formula_prop_needs_name))
            return FormulaExpression.PropertyReference(propertyName)
        }

        val function = FormulaFunction.entries.firstOrNull { it.formulaName.equals(name, ignoreCase = true) }
            ?: throw FormulaException(formulaPhrase(Res.string.message_formula_unknown_function, name))
        if (arguments.size !in function.argumentCounts) {
            throw FormulaException(function.argumentCountProblem)
        }
        if (function == FormulaFunction.IFS && arguments.size % 2 == 0) {
            throw FormulaException(formulaPhrase(Res.string.message_formula_ifs_needs_pairs))
        }
        return FormulaExpression.FunctionCall(function, arguments)
    }

    private fun parseArguments(): List<FormulaExpression> {
        if (takeSymbol(")")) return emptyList()
        val arguments = mutableListOf(parseComparison())
        while (takeSymbol(",")) arguments += parseComparison()
        expectSymbol(")")
        return arguments
    }

    private fun takeOperator(operators: List<FormulaOperator>): FormulaOperator? {
        val symbol = (tokens.getOrNull(position) as? FormulaToken.SymbolToken)?.symbol ?: return null
        val operator = operators.firstOrNull { it.symbol == symbol } ?: return null
        position++
        return operator
    }

    private fun takeSymbol(symbol: String): Boolean {
        val token = tokens.getOrNull(position)
        if (token !is FormulaToken.SymbolToken || token.symbol != symbol) return false
        position++
        return true
    }

    private fun expectSymbol(symbol: String) {
        if (!takeSymbol(symbol)) throw FormulaException(formulaPhrase(Res.string.message_formula_missing_symbol, symbol))
    }

    private fun describe(token: FormulaToken): FormulaMessage = when (token) {
        is FormulaToken.NumberToken -> formulaPhrase(Res.string.message_formula_piece_number)
        is FormulaToken.TextToken -> formulaPhrase(Res.string.message_formula_piece_text, formulaTextLiteral(token.text))
        is FormulaToken.NameToken -> formulaPhrase(Res.string.message_formula_piece_word, token.name)
        is FormulaToken.SymbolToken -> formulaPhrase(Res.string.message_formula_piece_symbol, token.symbol)
    }
}
