// ---------------------------------------------------------------------------
//
//  TilePacker
//  Copyright (c) 2003,2005,2009  Peter Heinrich
//
//  This program is free software; you can redistribute it and/or
//  modify it under the terms of the GNU General Public License
//  as published by the Free Software Foundation; either version 2
//  of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Boston, MA  02110-1301, USA.
//
// ---------------------------------------------------------------------------



package com.saphum.tools;



import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;



/**
 *  Combines separate image files into a single tileset, exporting a image
 *  array and position/extent metadata.
 *
 *  @author pheinrich
 *  @version 1
 */
public class TilePacker
{
   /**
    *  The default output file name, if none is provided.
    */
   public static final String DEFAULT_OUT_PATH = "out.png";
   /**
    *  The default maximum tile width, if none is provided.  This process will
    *  fail if the maximum width specified (implicitly or explicitly) is smaller
    *  than one or more of the component tiles.
    */
   public static final int DEFAULT_MAX_WIDTH = 1024;

   private BufferedImage[] m_aImages;
   private Rectangle[] m_aExtents;

   /**
    *  Constructor.  The image array may be reordered during the packing process
    *  (sorted by decreasing width, for example).  The <code>aExtents</code>
    *  parameter will be filled with the resultant location (within the new tile)
    *  of each individual image.
    *
    *  @param aImages an array of source images
    *  @param aExtents an array of rectangles to receive position information
    */
   public TilePacker( BufferedImage[] aImages, Rectangle[] aExtents )
   {
      m_aImages = aImages;
      m_aExtents = aExtents;

      //  Sort the images in descending order by area.
      Arrays.sort( m_aImages, new Comparator()
         {
            public int compare( Object o1, Object o2 )
            {
               int nArea1 = ((BufferedImage)o1).getWidth() * ((BufferedImage)o1).getHeight();
               int nArea2 = ((BufferedImage)o2).getWidth() * ((BufferedImage)o2).getHeight();

               return( nArea2 - nArea1 );
            }
         } );
   }

   /**
    *  Main entry point for use by external consumers.
    *
    *  @param asArgs an array of command line arguments
    */
   public static void main( String[] asArgs )
   {
      try
      {
         final ArrayList lFiles = new ArrayList();
         String sOutPath = DEFAULT_OUT_PATH;
         int nMaxWidth = DEFAULT_MAX_WIDTH;

         //  Scan the command line arguments for source images and an output name.
         for( int i = 0; i < asArgs.length; i++ )
         {
            if( asArgs[ i ].equalsIgnoreCase( "-h" ) )
            {
               //  Display a usage string and exit.
               System.out.println( "Usage: TilePacker [-h] [-w maxwidth] [-o outfile] <file1> <file2> ...\n" +
                                   "If no outfile is specified, \"" + sOutPath + "\" is used.\n" +
                                   "The default maximum width is " + nMaxWidth + " pixels.\n" );
               System.exit( 0 );
            }
            else if( asArgs[ i ].equalsIgnoreCase( "-o" ) )
            {
               //  Set the output filename.  Subsequent occurrences of the -o switch will
               //  override the value we set here.
               sOutPath = asArgs[ ++i ];
            }
            else if( asArgs[ i ].equalsIgnoreCase( "-w" ) )
            {
               //  Set the maximum tile width.  Subsequent occurrences of the -w switch will
               //  override the value we set here.
               nMaxWidth = Integer.parseInt( asArgs[ ++i ] );
            }
            else
            {
               //  Bare parameters are taken to be image file names.
               lFiles.add( new File( asArgs[ i ] ) );
            }
         }

         //  Do nothing if no source images were specified.
         if( 0 < lFiles.size() )
         {
            HashMap hImageToPath = new HashMap();

            //  Load all the images specified on the command line.
            for( int i = 0; i < lFiles.size(); i++ )
            {
               File file = (File)lFiles.get( i );

               try
               {
                  //  Map the image object to the path used to load it.
                  System.out.println( "Reading " + file.getName() );
                  BufferedImage image = ImageIO.read( file );
                  hImageToPath.put( image, file.getName() );
               }
               catch( ArrayIndexOutOfBoundsException aioobe )
               {
                  //  The ImageIO library complains if the number of palette entries doesn't
                  //  equal 2^BPP, where BPP is the bits-per-pixel.  (This is an error in the
                  //  library, however; the PNG format can certainly handle the situation.)
                  System.out.println( "Chunk problem with " + file.getAbsolutePath() +
                                      ".  Check palette size against BPP." );
               }
            }

            BufferedImage[] aImages = (BufferedImage[])hImageToPath.keySet().toArray( new BufferedImage[ 0 ] );
            Rectangle[] aExtents = new Rectangle[ aImages.length ];

            for( int i = 0; i < aExtents.length; i++ )
               aExtents[ i ] = new Rectangle();

            //  Generate a "tiled" image from the source images specified. The image order
            //  within the array parameter may change, but the corresponding extent info
            //  will also move, so the two will still line up.
            TilePacker tp = new TilePacker( aImages, aExtents );
            BufferedImage tileset = tp.pack( nMaxWidth );

            if( null != tileset )
               ImageIO.write( tileset, "PNG", new File( sOutPath ) );

            //  Write out the metadata.  This could be more elegant, but is it necessary?
            System.out.println( "<tileset image=\"" + sOutPath + "\">" );
            for( int i = 0; i < aImages.length; i++ )
            {
               String sPath = (String)hImageToPath.get( aImages[ i ] );
               Rectangle rExtent = aExtents[ i ];

               System.out.print( "   <tile id=\"" + sPath );
               System.out.print( "\" x=\"" + rExtent.x + "\" y=\"" + rExtent.y );
               System.out.print( "\" w=\"" + rExtent.width + "\" h=\"" + rExtent.height );
               System.out.println( "\"/>" );
            }
            System.out.println( "</tileset>" );
         }
         else
         {
            System.out.println( "Error:  No source images specified.  Use -h to display usage." );
            System.exit( 1 );
         }
      }
      catch( IOException ioe )
      {
         System.out.println( "Error: " + ioe.getMessage() );
         ioe.printStackTrace();
      }
   }

   /**
    *  Processes an array of source images into a single "tiled" image.  That is,
    *  the images are globbed together so they share a single CLUT, transparency
    *  chunk, etc.<p/>
    *
    *  @param nMaxWidth the maximum desired tile size
    *  @return a <code>BufferedImage</code> containing all the source images
    *  combined together
    *  @throws IOException if at least one image exceeds the maximum width
    */
   public BufferedImage pack( int nMaxWidth )
      throws IOException
   {
      int nMinArea = Integer.MAX_VALUE;
      int nBestWidth = nMaxWidth;
      int nWidth = 0;

      //  Find the widest individual source image.
      for( int i = 0; i < m_aImages.length; i++ )
      {
         if( nWidth < m_aImages[ i ].getWidth() )
            nWidth = m_aImages[ i ].getWidth();
      }

      if( nWidth > nMaxWidth )
         throw new IOException( "At least one image exceeds maximum width" );

      //  Now shuffle the images into progressively wider tiles, starting with one
      //  as wide as the widest individual image, but never wider than the maximum
      //  specified by the caller.
      while( nWidth < nMaxWidth )
      {
         int nArea = rearrangeForWidth( nWidth );

         //  Choose the width resulting in the smallest area and shortest perimeter.
         if( nArea < nMinArea || (nArea == nMinArea &&
             (nWidth + (nArea / nWidth)) < (nBestWidth + (nMinArea / nBestWidth))) )
         {
            nMinArea = nArea;
            nBestWidth = nWidth;
         }

         //  Increase the width of the tile and try again.
         nWidth++;
      }

      //  Finalize the layout using the best width we could find.
      nMinArea = rearrangeForWidth( nBestWidth );

      BufferedImage tile = new BufferedImage( nBestWidth, nMinArea / nBestWidth, BufferedImage.TYPE_4BYTE_ABGR );
      Graphics graphics = tile.getGraphics();

      //  Draw the indiviual images into a single bitmap.
      for( int i = 0; i < m_aImages.length; i++ )
      {
         BufferedImage image = m_aImages[ i ];
         Rectangle rExtent = m_aExtents[ i ];

         graphics.drawImage( image, rExtent.x, rExtent.y, null );
      }

      return( tile );
   }

   /**
    *  Processes all images loaded so far, trying to shuffle them into a tile of
    *  the specified width.  Since this method may extend the target image to an
    *  arbitrary height, the total area of the result is returned.
    *
    *  @param nWidth the width in pixels of the target image
    *  @return the total area of the resultant image
    */
   private int rearrangeForWidth( int nWidth )
   {
      ArrayList lHoles = new ArrayList();
      int nMaxHeight = 0;

      //  The destination bitmap starts as one huge hole (new images may be placed
      //  anywhere).  As we add images, the hole shrinks and new holes are added.
      //  These will usually overlap at least one existing hole in one direction.
      //
      //  Make the starting hole's height big, but not too big.  We don't want
      //  future calculations to overflow.
      lHoles.add( new Rectangle( 0, 0, nWidth, Integer.MAX_VALUE >> 1 ) );

      //  Process all images sequentially (they're already arranged in decreasing
      //  order by total area).
      for( int i = 0; i < m_aImages.length; i++ )
      {
         BufferedImage image = m_aImages[ i ];
         Rectangle rExtent = m_aExtents[ i ];
         rExtent.setSize( image.getWidth(), image.getHeight() );

         //  Compare the current image to every hole, looking for one big enough to hold
         //  it.  The holes are arranged by vertical position (highest first), then
         //  horizontal position (left-most first).
         for( int j = 0; j < lHoles.size(); j++ )
         {
            Rectangle rHole = (Rectangle)lHoles.get( j );

            if( rHole.width >= rExtent.width && rHole.height >= rExtent.height )
            {
               ArrayList lNewHoles = new ArrayList();

               //  This one will do, so position the image in the upper left corner of the
               //  hole we found.
               rExtent.setLocation( rHole.x, rHole.y );

               //  Now compare the image--in its new position--to all the holes we know about,
               //  because its presence may require some to be broken into smaller holes.
               for( int k = 0; k < lHoles.size(); k++ )
               {
                  Rectangle rScan = (Rectangle)lHoles.get( k );

                  //  Do nothing if the image doesn't overlap the current hole.
                  if( rScan.intersects( rExtent ) )
                  {
                     //  Top edge of image (in new position) is below this hole's top;
                     //  i.e. there's a new hole above.
                     if( rScan.y < rExtent.y && rExtent.y < rScan.y + rScan.height )
                        lNewHoles.add( new Rectangle( rScan.x, rScan.y, rScan.width, rExtent.y - rScan.y ) );

                     //  Left edge of image is to the right of this hole's left edge;
                     //  i.e. there's a new hole to the left.
                     if( rScan.x < rExtent.x )
                        lNewHoles.add( new Rectangle( rScan.x, rScan.y, rExtent.x - rScan.x, rScan.height ) );

                     //  Right edge of image is to the left of this hole's right edge;
                     //  i.e. there's a new hole to the right.
                     if( rScan.x + rScan.width > rExtent.x + rExtent.width )
                     {
                        int nLeft = rExtent.x + rExtent.width;
                        lNewHoles.add( new Rectangle( nLeft, rScan.y, rScan.x + rScan.width - nLeft, rScan.height ) );
                     }

                     //  Bottom edge of image is above this hole's bottom;
                     //  i.e. there's a new hole below.
                     if( rScan.y + rScan.height > rExtent.y + rExtent.height )
                     {
                        int nTop = rExtent.y + rExtent.height;
                        lNewHoles.add( new Rectangle( rScan.x, nTop, rScan.width, rScan.y + rScan.height - nTop ) );
                     }
                  }
                  else
                     lNewHoles.add( rScan );
               }

               //  Trim down the new list of holes by pruning those that are wholly contained.
               lHoles.clear();
               lHoles.addAll( collapseHoles( lNewHoles ) );

               if( rExtent.y + rExtent.height > nMaxHeight )
                  nMaxHeight = rExtent.y + rExtent.height;
               break;
            }
         }
      }

      return( nWidth * nMaxHeight );
   }

   /**
    *  Copies bounded extents from one list to another, pruning any that are wholly
    *  contained.  The resulting collection will be sorted in order of ascending
    *  rows and columns, i.e. sort low-y then low-x.
    *
    *  @param lFrom the source list of <code>Rectangle</code> objects
    *  @return a <code>List</code> of the non-wholly-contained rectangles
    */
   private List collapseHoles( ArrayList lFrom )
   {
      int nHoles = lFrom.size();

      //  Process all the holes in the list.
      for( int i = 0; i < lFrom.size(); i++ )
      {
         Rectangle hole = (Rectangle)lFrom.get( i );

         //  null is a valid value, since we may remove holes from list as they're
         //  collapsed and coalesced.
         if( null != hole )
         {
            for( int j = 1 + i; j < lFrom.size(); j++ )
            {
               Rectangle comparand = (Rectangle)lFrom.get( j );
               long nHoleArea = (long)hole.width * hole.height;

               //  Above comment applies to all following holes, too, of course.
               if( null != comparand )
               {
                  Rectangle union = hole.union( comparand );
                  Rectangle intersection = hole.intersection( comparand );

                  long nComparandArea = (long)comparand.width * comparand.height;
                  long nUnionArea = (long)union.width * union.height;
                  long nIntersectionArea = (long)intersection.width * intersection.height;

                  //  If, when taken together, the holes cover the same area (or less) as when
                  //  taken separately, either they're equal, one contains the other, or they
                  //  share at least one edge and corresponding extent.
                  if( nUnionArea <= nHoleArea + nComparandArea - nIntersectionArea )
                  {
                     //  Combine the holes.
                     lFrom.set( i, union );
                     lFrom.set( j, null );
                     nHoles--;
                  }
               }
            }
         }
      }

      Rectangle[] aTo = new Rectangle[ nHoles ];
      nHoles = 0;

      //  Initialize an array of the holes that remain (they're non-null) so we can
      //  sort them with the Arrays utility class.
      for( int i = 0; i < lFrom.size(); i++ )
      {
         if( null != lFrom.get( i ) )
            aTo[ nHoles++ ] = (Rectangle)lFrom.get( i );
      }

      //  Sort the holes, lowest y then lowest x coordinates.
      Arrays.sort( aTo, 0, nHoles, new Comparator()
         {
            public int compare( Object o1, Object o2 )
            {
               Rectangle r1 = (Rectangle)o1;
               Rectangle r2 = (Rectangle)o2;

               return( (r1.y == r2.y) ? (r1.x - r2.x) : (r1.y - r2.y) );
            }
         } );

      return( Arrays.asList( aTo ) );
   }
}
