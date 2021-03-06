/*
 * Sonar PL/SQL Plugin (Community)
 * Copyright (C) 2015-2017 Felipe Zorzo
 * mailto:felipebzorzo AT gmail DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plsqlopen;

import com.sonar.sslr.api.Token;

public class TokenLocation {
    
    private int line;
    private int column;
    private int endLine;
    private int endColumn;
    
    private TokenLocation(int line, int column, int endLine, int endColumn) {
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.endColumn = endColumn;
    }
    
    public int line() {
        return line;
    }
    
    public int column() {
        return column;
    }
    
    public int endLine() {
        return endLine;
    }
    
    public int endColumn() {
        return endColumn;
    }
    
    public static TokenLocation from(Token token) {
        String[] lines = token.getValue().split("\\r\\n|\\n|\\r");
        int endLineOffset = token.getColumn() + token.getValue().length();
        int endLine = token.getLine() + lines.length - 1;
        if (endLine != token.getLine()) {
            endLineOffset = lines[lines.length - 1].length();
        }
        return new TokenLocation(token.getLine(), token.getColumn(), endLine, endLineOffset);
    }

}
