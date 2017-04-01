/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.desktop.ui;

import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;

import java.awt.Component;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

@SuppressWarnings("MagicConstant")
public class ScrollableOptionPane
{
    public static void showWrappedMessageDialog( Component parentComponent, String message, String title,
                                                 int messageType )
    {
        showMessageDialog( parentComponent, createWrappingScrollPane( message ), title, messageType );
    }

    public static int showWrappedConfirmDialog( Component parentComponent, String message, String title,
                                                int optionType, int messageType )
    {
        return showConfirmDialog( parentComponent, createWrappingScrollPane( message ), title, optionType, messageType );
    }

    private static JScrollPane createWrappingScrollPane( String message )
    {
        JTextArea view = new JTextArea( message, 10, 80 );
        view.setLineWrap( true );
        view.setEditable( false );
        view.setWrapStyleWord( true );
        return new JScrollPane( view );
    }
}