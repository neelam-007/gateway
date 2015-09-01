package com.l7tech.external.assertions.generatepassword.server;

/**
 * Generate random characters for password
 *
 * @author rraquepo, 4/9/14
 */
public class GeneratePassword {
    static final String VALID_NUMBER_CHARS = "23456789"; //notice we are not including 0 and 1 (zero and one)
    static final String VALID_LOWERCASE_CHARS = "abcdefghjkmnpqrstuvwxyz"; //notice we are not including l and o (el and oh)
    static final String VALID_UPPERCASE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ"; //notice we are not including L and O (el and oh)
    static final String VALID_SPECIAL_CHARS = "!@#$%";
    static final int MAX_PASSWORD_LENGTH = 256;//prevents infinite loop

    public static String generatePassword(int number_length, int lowercase_length, int specialchar_length, int uppercase_length, int max_length) {
        int number_counter = 0, lowercase_counter = 0, uppercase_counter = 0, specialchar_counter = 0;
        StringBuffer password = new StringBuffer();
        int lastChar = -1;
        //we will generate one type of char at a time, this will prevent repeating character
        while (true) {
            if (number_counter < number_length) {
                char newChar = VALID_NUMBER_CHARS.charAt(getRandomNum(0, VALID_NUMBER_CHARS.length() - 1));
                while (newChar == lastChar && lastChar != -1) {
                    newChar = VALID_NUMBER_CHARS.charAt(getRandomNum(0, VALID_NUMBER_CHARS.length() - 1));
                }
                lastChar = newChar;
                password.append(newChar);
                number_counter++;
                if (isMaxReached(number_counter, lowercase_counter, specialchar_counter, uppercase_counter, max_length)) {
                    break;
                }
            }
            if (lowercase_counter < lowercase_length) {
                char newChar = VALID_LOWERCASE_CHARS.charAt(getRandomNum(0, VALID_LOWERCASE_CHARS.length() - 1));
                while (newChar == lastChar && lastChar != -1) {
                    newChar = VALID_LOWERCASE_CHARS.charAt(getRandomNum(0, VALID_LOWERCASE_CHARS.length() - 1));
                }
                lastChar = newChar;
                password.append(newChar);
                lowercase_counter++;
                if (isMaxReached(number_counter, lowercase_counter, specialchar_counter, uppercase_counter, max_length)) {
                    break;
                }
            }
            if (specialchar_counter < specialchar_length) {
                char newChar = VALID_SPECIAL_CHARS.charAt(getRandomNum(0, VALID_SPECIAL_CHARS.length() - 1));
                while (newChar == lastChar && lastChar != -1) {
                    newChar = VALID_SPECIAL_CHARS.charAt(getRandomNum(0, VALID_SPECIAL_CHARS.length() - 1));
                }
                lastChar = newChar;
                password.append(newChar);
                specialchar_counter++;
                if (isMaxReached(number_counter, lowercase_counter, specialchar_counter, uppercase_counter, max_length)) {
                    break;
                }
            }
            if (uppercase_counter < uppercase_length) {
                char newChar = VALID_UPPERCASE_CHARS.charAt(getRandomNum(0, VALID_UPPERCASE_CHARS.length() - 1));
                while (newChar == lastChar && lastChar != -1) {
                    newChar = VALID_UPPERCASE_CHARS.charAt(getRandomNum(0, VALID_UPPERCASE_CHARS.length() - 1));
                }
                lastChar = newChar;
                password.append(newChar);
                uppercase_counter++;
                if (isMaxReached(number_counter, lowercase_counter, specialchar_counter, uppercase_counter, max_length)) {
                    break;
                }
            }
            if ((number_counter >= number_length
                    && lowercase_counter >= lowercase_length
                    && uppercase_counter >= uppercase_length
                    && specialchar_counter >= specialchar_length)
                    ) {
                break;
            }
            if (isMaxReached(number_counter, lowercase_counter, specialchar_counter, uppercase_counter, max_length)) {
                break;
            }
        }
        return password.toString();
    }

    protected static boolean isMaxReached(int number_counter, int lowercase_counter, int specialchar_counter, int uppercase_counter, int max_length) {
        return (number_counter + lowercase_counter + specialchar_counter + uppercase_counter) >= max_length;
    }

    protected static int getRandomNum(int lbound, int ubound) {
        return (int) (Math.floor(Math.random() * (ubound - lbound)) + lbound);
    }
}
